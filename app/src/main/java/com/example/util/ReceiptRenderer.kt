package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Bill
import com.example.data.model.StoreSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptRenderer {

    fun generateReceiptBitmap(bill: Bill, settings: StoreSettings): Bitmap {
        val width = 600
        val baseHeight = 900 + (bill.items.size * 38)
        val bitmap = Bitmap.createBitmap(width, baseHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean white receipt canvas
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
        }

        var y = 45f

        // Top Store Header
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.parseColor("#12355B") // Deep Navy
        canvas.drawText(settings.storeName, width / 2f, y, paint)
        y += 28f

        paint.color = Color.DKGRAY
        paint.textSize = 17f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(settings.address, width / 2f, y, paint)
        y += 24f

        canvas.drawText("Phone: ${settings.phone} | GST: ${settings.gstin}", width / 2f, y, paint)
        y += 30f

        // Separator line
        paint.color = Color.LTGRAY
        paint.strokeWidth = 2f
        canvas.drawLine(24f, y, width - 24f, y, paint)
        y += 26f

        // Bill Meta
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

        val sdf = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        val dateStr = sdf.format(Date(if (bill.paidAt > 0) bill.paidAt else bill.createdAt))

        canvas.drawText("BILL NO : ${bill.billId}", 24f, y, paint)
        y += 22f
        canvas.drawText("DATE    : $dateStr", 24f, y, paint)
        y += 22f
        canvas.drawText("COUNTER : ${bill.counterId}  | CASHIER: ${bill.cashierId}", 24f, y, paint)
        y += 22f
        if (bill.customerPhone.isNotBlank()) {
            canvas.drawText("CUSTOMER: ${bill.customerPhone}", 24f, y, paint)
            y += 22f
        }

        // Table Header
        y += 10f
        paint.color = Color.parseColor("#12355B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawRect(24f, y - 18f, width - 24f, y + 14f, Paint().apply { color = Color.parseColor("#F1F5F9") })
        canvas.drawText("ITEM", 28f, y, paint)
        canvas.drawText("QTY", 320f, y, paint)
        canvas.drawText("RATE", 400f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("TOTAL", width - 28f, y, paint)
        y += 26f

        // Items
        paint.color = Color.BLACK
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 16f

        for (item in bill.items) {
            paint.textAlign = Paint.Align.LEFT
            val itemName = if (item.name.length > 22) item.name.take(20) + ".." else item.name
            canvas.drawText(itemName, 28f, y, paint)
            canvas.drawText("${item.qty}", 330f, y, paint)
            canvas.drawText("₹%.1f".format(item.unitPrice), 400f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("₹%.2f".format(item.lineTotal), width - 28f, y, paint)
            y += 32f
        }

        // Separator
        y += 8f
        paint.color = Color.LTGRAY
        canvas.drawLine(24f, y, width - 24f, y, paint)
        y += 26f

        // Subtotals & Grand Total
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 17f
        canvas.drawText("Subtotal", 28f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹%.2f".format(bill.subtotal), width - 28f, y, paint)
        y += 24f

        if (bill.taxTotal > 0) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Taxes (GST)", 28f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("₹%.2f".format(bill.taxTotal), width - 28f, y, paint)
            y += 24f
        }

        if (bill.discount > 0) {
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Discount", 28f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("-₹%.2f".format(bill.discount), width - 28f, y, paint)
            y += 24f
        }

        // Grand Total Box
        y += 6f
        val totalBoxPaint = Paint().apply { color = Color.parseColor("#12355B") }
        canvas.drawRoundRect(24f, y - 20f, width - 24f, y + 26f, 12f, 12f, totalBoxPaint)
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("GRAND TOTAL (${bill.paymentMode})", 38f, y + 6f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("₹%.2f".format(bill.grandTotal), width - 38f, y + 6f, paint)
        y += 50f

        // Secure Exit Gate QR Code
        val qrContent = if (bill.receiptToken.isNotBlank()) {
            bill.receiptToken
        } else {
            HmacHelper.buildReceiptQrContent(bill.billId, bill.createdAt, settings.hmacSecret)
        }
        val qrBitmap = QrCodeGenerator.generateBitmap(qrContent, 200, 2)
        val qrLeft = (width - 200) / 2
        canvas.drawBitmap(qrBitmap, qrLeft.toFloat(), y, null)
        y += 215f

        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.parseColor("#12355B")
        paint.textSize = 17f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("[ SHOW THIS QR AT EXIT GATE ]", width / 2f, y, paint)
        y += 20f

        paint.color = Color.DKGRAY
        paint.textSize = 14f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Counter Ref: ${bill.counterId} | Sec: ${bill.billId.takeLast(6)}", width / 2f, y, paint)
        y += 26f

        paint.textSize = 15f
        paint.color = Color.parseColor("#1B998B")
        canvas.drawText(settings.receiptFooter, width / 2f, y, paint)

        return bitmap
    }

    fun saveReceiptToCache(context: Context, bitmap: Bitmap, billId: String): File {
        val imagesDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val imageFile = File(imagesDir, "receipt_$billId.png")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return imageFile
    }

    fun shareReceiptWhatsApp(context: Context, bill: Bill, settings: StoreSettings) {
        try {
            val bitmap = generateReceiptBitmap(bill, settings)
            val file = saveReceiptToCache(context, bitmap, bill.billId)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Receipt for Bill ${bill.billId} - Total: ₹${"%.2f".format(bill.grandTotal)} from ${settings.storeName}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // If phone provided, can target WhatsApp
                val cleanPhone = bill.customerPhone.replace("[^0-9]".toRegex(), "")
                if (cleanPhone.length >= 10) {
                    val phoneWithCode = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                    putExtra("jid", "$phoneWithCode@s.whatsapp.net")
                }
                setPackage("com.whatsapp")
            }

            try {
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                // If WhatsApp package not found, open general share sheet
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Receipt for Bill ${bill.billId} - Total: ₹${"%.2f".format(bill.grandTotal)} from ${settings.storeName}"
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(genericIntent, "Share Receipt via"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareReceiptSms(context: Context, bill: Bill, settings: StoreSettings) {
        val cleanPhone = bill.customerPhone.replace("[^0-9]".toRegex(), "")
        val smsText = "Thank you for shopping at ${settings.storeName}! Bill ${bill.billId}: ₹${"%.2f".format(bill.grandTotal)}. Show Bill ID or digital receipt at exit."
        val smsUri = Uri.parse("smsto:$cleanPhone")
        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", smsText)
        }
        try {
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:$cleanPhone")
                putExtra("sms_body", smsText)
            }
            context.startActivity(Intent.createChooser(fallback, "Send SMS"))
        }
    }
}
