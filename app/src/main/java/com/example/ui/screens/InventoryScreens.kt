package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.data.model.Product
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.QrCodeImage
import com.example.ui.components.SmartBillingTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.UiScreen
import com.example.util.PdfReportGenerator

@Composable
fun InventoryHomeScreen(
    viewModel: BillingViewModel,
    onOpenQrPrint: () -> Unit,
    onOpenAnalytics: () -> Unit
) {
    val products by viewModel.repository.products.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "In Stock", "Low Stock", "Out of Stock"
    var showAddDialog by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }
    var selectedProductForAdjust by remember { mutableStateOf<Product?>(null) }

    val lowStockItems = products.filter { it.stockQty in 1..it.reorderLevel }
    val outOfStockItems = products.filter { it.stockQty <= 0 }

    val filteredProducts = products.filter { p ->
        val matchesSearch = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true) || p.sku.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "In Stock" -> p.stockQty > p.reorderLevel
            "Low Stock" -> p.stockQty in 1..p.reorderLevel
            "Out of Stock" -> p.stockQty <= 0
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Inventory & Products",
                department = Department.INVENTORY,
                onSwitchDepartment = { viewModel.navigateTo(UiScreen.DepartmentPortal) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = TealAccent,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(14.dp)
        ) {
            FeedbackBanner(feedbackMessage)

            // Low Stock Alert Banner (Section 6.1)
            if (lowStockItems.isNotEmpty() || outOfStockItems.isNotEmpty()) {
                Surface(
                    color = WarningOrangeLight,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = WarningOrange)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Alert: ${lowStockItems.size} items low stock, ${outOfStockItems.size} out of stock. Restock needed!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningOrange
                        )
                    }
                }
            }

            // Quick Actions: QR Printing & Analytics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenQrPrint,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("inventory_qr_print_btn")
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("A4 QR Stickers", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenAnalytics,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("inventory_analytics_btn")
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sales Charts", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search products by Name or SKU...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "In Stock", "Low Stock", "Out of Stock")) { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Products List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { prod ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = prod.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = DeepNavyPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = when {
                                            prod.stockQty <= 0 -> ErrorRedLight
                                            prod.stockQty <= prod.reorderLevel -> WarningOrangeLight
                                            else -> SuccessGreenLight
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                prod.stockQty <= 0 -> "OUT OF STOCK"
                                                prod.stockQty <= prod.reorderLevel -> "LOW STOCK"
                                                else -> "IN STOCK"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                prod.stockQty <= 0 -> ErrorRed
                                                prod.stockQty <= prod.reorderLevel -> WarningOrange
                                                else -> SuccessGreen
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "SKU: ${prod.sku} • Category: ${prod.categoryId}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                Text(
                                    text = "Selling: ₹%.2f (MRP: ₹%.2f) • Stock: ${prod.stockQty} ${prod.unit}".format(prod.sellPrice, prod.mrp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TealAccent
                                )
                            }

                            // Quick Stock In / Adjust Button
                            Button(
                                onClick = {
                                    selectedProductForAdjust = prod
                                    showAdjustDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = DeepNavyPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Adjust", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Product Dialog (Section 6.2)
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Groceries") }
        var sellPrice by remember { mutableStateOf("") }
        var mrp by remember { mutableStateOf("") }
        var costPrice by remember { mutableStateOf("") }
        var stockQty by remember { mutableStateOf("") }
        var taxPercent by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Product", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sellPrice,
                            onValueChange = { sellPrice = it },
                            label = { Text("Sell Price (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mrp,
                            onValueChange = { mrp = it },
                            label = { Text("MRP (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costPrice,
                            onValueChange = { costPrice = it },
                            label = { Text("Cost Price (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = stockQty,
                            onValueChange = { stockQty = it },
                            label = { Text("Opening Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && sellPrice.isNotBlank()) {
                            val nextSku = "SKU-%04d".format((1000..9999).random())
                            val sp = sellPrice.toDoubleOrNull() ?: 0.0
                            val mp = mrp.toDoubleOrNull() ?: sp
                            val cp = costPrice.toDoubleOrNull() ?: (sp * 0.8)
                            val sq = stockQty.toIntOrNull() ?: 10

                            val newProd = Product(
                                sku = nextSku,
                                name = name,
                                categoryId = category,
                                mrp = mp,
                                sellPrice = sp,
                                costPrice = cp,
                                stockQty = sq,
                                taxPercent = taxPercent.toDoubleOrNull() ?: 0.0
                            )
                            viewModel.saveProduct(newProd)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text("Save & Generate SKU")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Stock Adjust Dialog (Section 6.2)
    if (showAdjustDialog && selectedProductForAdjust != null) {
        val prod = selectedProductForAdjust!!
        var adjustType by remember { mutableStateOf("PURCHASE") } // "PURCHASE", "DAMAGE", "CORRECTION"
        var qtyText by remember { mutableStateOf("") }
        var reasonNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = { Text("Adjust Stock: ${prod.name}") },
            text = {
                Column {
                    Text("Current stock: ${prod.stockQty} ${prod.unit}", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = adjustType == "PURCHASE",
                            onClick = { adjustType = "PURCHASE" },
                            label = { Text("+ Stock In", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = adjustType == "DAMAGE",
                            onClick = { adjustType = "DAMAGE" },
                            label = { Text("- Damage", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = adjustType == "CORRECTION",
                            onClick = { adjustType = "CORRECTION" },
                            label = { Text("Correction", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = reasonNote,
                        onValueChange = { reasonNote = it },
                        label = { Text("Reason / Invoice No / Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = qtyText.toIntOrNull() ?: 0
                        val signedQty = if (adjustType == "DAMAGE") -q else q
                        viewModel.adjustStock(prod.sku, signedQty, adjustType, reasonNote)
                        showAdjustDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyPrimary)
                ) {
                    Text("Apply Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdjustDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InventoryQrPrintScreen(
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.repository.products.collectAsState()
    val settings by viewModel.repository.settings.collectAsState()

    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var isBulkSheetOn by remember { mutableStateOf(true) } // Section 6.3: Bulk Sheet toggle

    val product = selectedProduct ?: products.firstOrNull()

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Product QR Sticker Generator",
                department = Department.INVENTORY,
                onSwitchDepartment = { viewModel.navigateTo(UiScreen.DepartmentPortal) },
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Product Selector Dropdown / Chips
            Text("Select product to print QR sticker:", fontWeight = FontWeight.Bold, color = DeepNavyPrimary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(products) { p ->
                    FilterChip(
                        selected = p.sku == product?.sku,
                        onClick = { selectedProduct = p },
                        label = { Text(p.name.take(18), fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bulk Sheet Toggle (Section 6.3)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Bulk A4 Sheet (10 QR Grid)", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                        Text(
                            text = if (isBulkSheetOn) "Generates 2x5 grid on A4 with cut lines" else "Single QR sticker card",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isBulkSheetOn,
                        onCheckedChange = { isBulkSheetOn = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (product != null) {
                // Preview Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBulkSheetOn) "PREVIEW: A4 Sheet (10 Stickers)" else "PREVIEW: Single Sticker Card",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TealAccent
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // QR Code Image
                        QrCodeImage(qrText = product.sku, sizeDp = 160)

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DeepNavyPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text("SKU: ${product.sku}", fontSize = 12.sp, color = Color.Gray)
                        Text("Selling Price: ₹%.2f (MRP: ₹%.2f)".format(product.sellPrice, product.mrp), fontWeight = FontWeight.Bold, color = TealAccent)

                        if (isBulkSheetOn) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sheet contains 10 identical cut-out stickers ready for thermal / A4 printing",
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Button: Export & Share PDF (Section 6.3 & 8)
                Button(
                    onClick = {
                        val pdfFile = PdfReportGenerator.generateBulkQrSheetA4(context, product, settings)
                        PdfReportGenerator.shareFile(context, pdfFile, "application/pdf", "Product QR Sheet - ${product.name}")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyPrimary),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("export_a4_pdf_btn")
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBulkSheetOn) "Export & Print A4 PDF (10 Copies)" else "Share QR Sticker PDF",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryAnalyticsScreen(
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.repository.products.collectAsState()
    val bills by viewModel.repository.bills.collectAsState()

    val categoryGroup = products.groupBy { it.categoryId }
    val totalStockUnits = products.sumOf { it.stockQty }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Inventory Analytics",
                department = Department.INVENTORY,
                onSwitchDepartment = { viewModel.navigateTo(UiScreen.DepartmentPortal) },
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(16.dp)
        ) {
            Text("Stock Breakdown by Category", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    categoryGroup.forEach { (category, list) ->
                        val catStock = list.sumOf { it.stockQty }
                        val fraction = if (totalStockUnits > 0) catStock.toFloat() / totalStockUnits else 0f

                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(category, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("$catStock units (${(fraction * 100).toInt()}%)", fontSize = 12.sp, color = TealAccent)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = TealAccent,
                                trackColor = Color(0xFFF1F5F9)
                            )
                        }
                    }
                }
            }

            Text("Top Products by Catalog Volume", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products.sortedByDescending { it.stockQty }.take(6)) { p ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("SKU: ${p.sku}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("${p.stockQty} in stock", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                        }
                    }
                }
            }
        }
    }
}
