package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Bill
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    /**
     * Builds an A4 sheet with 10 copies of the product QR in a neat 2 x 5 grid
     * with cut guidelines, product name, price and SKU underneath (Section 6.3).
     */
    fun generateBulkQrSheetA4(context: Context, product: Product, settings: StoreSettings): File {
        val pdfDocument = PdfDocument()
        // Standard A4 dimensions in points: 595 x 842 pt (72 dpi)
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#B0BEC5")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#12355B")
            textAlign = Paint.Align.CENTER
        }

        // Header
        textPaint.textSize = 15f
        textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("${settings.storeName} - Product QR Label Sheet (A4)", pageWidth / 2f, 32f, textPaint)

        // 2 columns x 5 rows = 10 labels
        val cols = 2
        val rows = 5
        val marginX = 35f
        val marginY = 50f
        val cellWidth = (pageWidth - (marginX * 2)) / cols // ~262 pt
        val cellHeight = (pageHeight - marginY - 30f) / rows // ~152 pt

        val qrBitmap = QrCodeGenerator.generateBitmap(product.sku, 160, 2)

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val left = marginX + (c * cellWidth)
                val top = marginY + (r * cellHeight)
                val right = left + cellWidth
                val bottom = top + cellHeight

                // Draw dashed cut guideline
                canvas.drawRect(left, top, right, bottom, borderPaint)

                // Draw QR Code centered in upper portion of cell
                val qrSize = 100f
                val qrLeft = left + (cellWidth - qrSize) / 2f
                val qrTop = top + 10f
                val scaledQr = Bitmap.createScaledBitmap(qrBitmap, qrSize.toInt(), qrSize.toInt(), true)
                canvas.drawBitmap(scaledQr, qrLeft, qrTop, null)

                // Product Name
                textPaint.textSize = 11f
                textPaint.color = Color.BLACK
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                val displayName = if (product.name.length > 26) product.name.take(24) + ".." else product.name
                canvas.drawText(displayName, left + cellWidth / 2f, qrTop + qrSize + 14f, textPaint)

                // SKU & Price
                textPaint.textSize = 10f
                textPaint.color = Color.parseColor("#546E7A")
                textPaint.typeface = android.graphics.Typeface.DEFAULT
                canvas.drawText("SKU: ${product.sku}", left + cellWidth / 2f, qrTop + qrSize + 26f, textPaint)

                textPaint.textSize = 12f
                textPaint.color = Color.parseColor("#1B998B")
                textPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("₹%.2f (MRP: ₹%.2f)".format(product.sellPrice, product.mrp), left + cellWidth / 2f, qrTop + qrSize + 39f, textPaint)
            }
        }

        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val pdfFile = File(reportsDir, "QR_Sheet_${product.sku}.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return pdfFile
    }

    /**
     * Builds Daily Sales Report PDF for the Store Manager.
     */
    fun generateDailyReportPdf(context: Context, bills: List<Bill>, settings: StoreSettings): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        var y = 50f
        paint.color = Color.parseColor("#12355B")
        paint.textSize = 20f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText(settings.storeName, 40f, y, paint)
        y += 24f

        paint.color = Color.DKGRAY
        paint.textSize = 13f
        paint.typeface = android.graphics.Typeface.DEFAULT
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        canvas.drawText("Daily Summary Report - Generated: ${sdf.format(Date())}", 40f, y, paint)
        y += 35f

        val paidBills = bills.filter { it.paymentStatus == "PAID" }
        val totalSales = paidBills.sumOf { it.grandTotal }
        val upiSales = paidBills.filter { it.paymentMode == "UPI" }.sumOf { it.grandTotal }
        val cashSales = paidBills.filter { it.paymentMode == "CASH" }.sumOf { it.grandTotal }
        val totalItems = paidBills.sumOf { b -> b.items.sumOf { it.qty } }

        // Stats boxes
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(40f, y, pageWidth - 40f, y + 80f, 12f, 12f, paint)

        paint.color = Color.parseColor("#12355B")
        paint.textSize = 14f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("Total Sales: ₹%.2f".format(totalSales), 60f, y + 30f, paint)
        canvas.drawText("Total Bills: ${paidBills.size}", 320f, y + 30f, paint)
        canvas.drawText("UPI Sales: ₹%.2f".format(upiSales), 60f, y + 60f, paint)
        canvas.drawText("Cash Sales: ₹%.2f".format(cashSales), 320f, y + 60f, paint)
        y += 110f

        // Table Header
        paint.color = Color.parseColor("#12355B")
        paint.textSize = 12f
        canvas.drawText("BILL ID", 40f, y, paint)
        canvas.drawText("TIME", 160f, y, paint)
        canvas.drawText("COUNTER", 270f, y, paint)
        canvas.drawText("MODE", 370f, y, paint)
        canvas.drawText("TOTAL", 480f, y, paint)
        y += 16f
        canvas.drawLine(40f, y, pageWidth - 40f, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
        y += 20f

        paint.color = Color.BLACK
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.textSize = 11f
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        for (b in paidBills.take(25)) {
            canvas.drawText(b.billId, 40f, y, paint)
            canvas.drawText(timeFormat.format(Date(if (b.paidAt > 0) b.paidAt else b.createdAt)), 160f, y, paint)
            canvas.drawText(b.counterId, 270f, y, paint)
            canvas.drawText(b.paymentMode, 370f, y, paint)
            canvas.drawText("₹%.2f".format(b.grandTotal), 480f, y, paint)
            y += 22f
            if (y > pageHeight - 50) break
        }

        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val pdfFile = File(reportsDir, "Daily_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return pdfFile
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, title))
    }
}
