package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.MetricCard
import com.example.ui.components.SmartBillingTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.UiScreen
import com.example.util.PdfReportGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManagerDashboardScreen(
    viewModel: BillingViewModel,
    onOpenStaff: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenAudit: () -> Unit
) {
    val bills by viewModel.repository.bills.collectAsState()
    val products by viewModel.repository.products.collectAsState()
    val counters by viewModel.repository.counters.collectAsState()
    val gateScans by viewModel.repository.gateScans.collectAsState()
    val settings by viewModel.repository.settings.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    val paidBills = bills.filter { it.paymentStatus == "PAID" }
    val todaySales = paidBills.sumOf { it.grandTotal }
    val totalItemsSold = paidBills.sumOf { b -> b.items.sumOf { it.qty } }
    val averageBill = if (paidBills.isNotEmpty()) todaySales / paidBills.size else 0.0

    // Profit calculation: sum of (unit_price - cost_price) * qty - discounts (Section 7.1)
    val totalProfit = paidBills.sumOf { b ->
        val itemProfit = b.items.sumOf { (it.unitPrice - it.costPriceSnapshot) * it.qty }
        (itemProfit - b.discount).coerceAtLeast(0.0)
    }

    val upiSales = paidBills.filter { it.paymentMode == "UPI" }.sumOf { it.grandTotal }
    val cashSales = paidBills.filter { it.paymentMode == "CASH" }.sumOf { it.grandTotal }

    val checkedBillsCount = paidBills.count { it.checkedStatus == "YES" }
    val lowStockCount = products.count { it.stockQty <= it.reorderLevel }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Manager Control Center",
                department = Department.MANAGER,
                onSwitchDepartment = { viewModel.navigateTo(UiScreen.DepartmentPortal) }
            )
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

            // Top Quick Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("manager_settings_btn")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenStaff,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("manager_staff_btn")
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Staff", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenReports,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("manager_reports_btn")
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reports", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onOpenAudit,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Key KPI Metrics Grid (Section 7.1)
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(
                            title = "Today's Gross Sales",
                            value = "₹%.2f".format(todaySales),
                            subtitle = "${paidBills.size} bills issued",
                            icon = Icons.Default.TrendingUp,
                            iconColor = DeepNavyPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Estimated Profit",
                            value = "₹%.2f".format(totalProfit),
                            subtitle = "Net of cost prices",
                            icon = Icons.Default.Savings,
                            iconColor = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(
                            title = "Average Basket Value",
                            value = "₹%.2f".format(averageBill),
                            subtitle = "$totalItemsSold items sold total",
                            icon = Icons.Default.ShoppingBag,
                            iconColor = TealAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Exit Verification",
                            value = "$checkedBillsCount / ${paidBills.size}",
                            subtitle = "Checked at security gate",
                            icon = Icons.Default.VerifiedUser,
                            iconColor = if (checkedBillsCount == paidBills.size) SuccessGreen else WarningOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Payment Split Breakdown
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Payment Mode Split", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(TealAccent))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("UPI: ₹%.2f".format(upiSales), fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(DeepNavyPrimary))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cash: ₹%.2f".format(cashSales), fontSize = 13.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val upiFraction = if (todaySales > 0) (upiSales / todaySales).toFloat() else 0.5f
                            LinearProgressIndicator(
                                progress = { upiFraction },
                                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                color = TealAccent,
                                trackColor = DeepNavyPrimary
                            )
                        }
                    }
                }

                // Counter-wise Performance (Section 7.1)
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Counter-Wise Performance", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                            Spacer(modifier = Modifier.height(10.dp))

                            counters.forEach { ctr ->
                                val ctrBills = paidBills.filter { it.counterId == ctr.counterId }
                                val ctrSales = ctrBills.sumOf { it.grandTotal }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${ctr.counterId}: ${ctr.name}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text("${ctrBills.size} bills completed", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("₹%.2f".format(ctrSales), fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                                }
                                Divider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }

                // Backend Sync Status Indicator
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (settings.appsScriptUrl.isNotBlank()) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (settings.appsScriptUrl.isNotBlank()) SuccessGreen else WarningOrange
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (settings.appsScriptUrl.isNotBlank()) "Connected to Google Sheets" else "Standalone Local DB (Apps Script link empty)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = DeepNavyPrimary
                                )
                                Text(
                                    text = if (settings.appsScriptUrl.isNotBlank()) settings.appsScriptUrl.take(45) + "..." else "Tap 'Settings' to paste your deployed Apps Script URL",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagerSettingsScreen(
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.repository.settings.collectAsState()
    val testResult by viewModel.connectionTestResult.collectAsState()

    var appsScriptUrl by remember { mutableStateOf(settings.appsScriptUrl) }
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var storeName by remember { mutableStateOf(settings.storeName) }
    var address by remember { mutableStateOf(settings.address) }
    var phone by remember { mutableStateOf(settings.phone) }
    var gstin by remember { mutableStateOf(settings.gstin) }
    var upiVpa by remember { mutableStateOf(settings.upiVpa) }
    var upiPayeeName by remember { mutableStateOf(settings.upiPayeeName) }
    var receiptFooter by remember { mutableStateOf(settings.receiptFooter) }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Store Settings & Backend",
                department = Department.MANAGER,
                onSwitchDepartment = { viewModel.navigateTo(UiScreen.DepartmentPortal) },
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // GOOGLE APPS SCRIPT WEB APP CONNECTION (Critical feature user requested)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = TealAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Google Apps Script Backend Connection",
                                fontWeight = FontWeight.Bold,
                                color = DeepNavyPrimary,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Paste your deployed Google Apps Script Web App URL here (from script.google.com linked to your Google Sheet):",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = appsScriptUrl,
                            onValueChange = { appsScriptUrl = it },
                            label = { Text("Web App URL (https://script.google.com/...)") },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("apps_script_url_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.testAppsScriptConnection(appsScriptUrl, apiKey)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Web App Connection")
                        }

                        if (testResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = if (testResult!!.startsWith("SUCCESS")) SuccessGreenLight else ErrorRedLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = testResult!!,
                                    fontSize = 12.sp,
                                    color = if (testResult!!.startsWith("SUCCESS")) SuccessGreen else ErrorRed,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Store Profile & UPI Details
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Store Profile & UPI Payment Settings", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("Store Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Store Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = gstin,
                                onValueChange = { gstin = it },
                                label = { Text("GSTIN") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = upiVpa,
                                onValueChange = { upiVpa = it },
                                label = { Text("UPI VPA (ID)") },
                                placeholder = { Text("store@upi") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = upiPayeeName,
                                onValueChange = { upiPayeeName = it },
                                label = { Text("Payee Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = receiptFooter,
                            onValueChange = { receiptFooter = it },
                            label = { Text("Receipt Footer Message") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Save Settings Button
            item {
                Button(
                    onClick = {
                        val updated = settings.copy(
                            appsScriptUrl = appsScriptUrl.trim(),
                            apiKey = apiKey.trim(),
                            storeName = storeName.trim(),
                            address = address.trim(),
                            phone = phone.trim(),
                            gstin = gstin.trim(),
                            upiVpa = upiVpa.trim(),
                            upiPayeeName = upiPayeeName.trim(),
                            receiptFooter = receiptFooter.trim()
                        )
                        viewModel.repository.updateSettings(updated)
                        onBack()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyPrimary),
                    modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_settings_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Apply Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ManagerStaffScreen(
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    val counters by viewModel.repository.counters.collectAsState()
    val gates by viewModel.repository.gates.collectAsState()
    val users by viewModel.repository.users.collectAsState()

    var showAddCounterDialog by remember { mutableStateOf(false) }
    var showAddGateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Counters & Staff Management",
                department = Department.MANAGER,
                onSwitchDepartment = { viewModel.navigateTo(UiScreen.DepartmentPortal) },
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Counters Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Billing Counters (${counters.size})", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                    TextButton(onClick = { showAddCounterDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Counter")
                    }
                }
            }

            items(counters) { ctr ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${ctr.counterId}: ${ctr.name}", fontWeight = FontWeight.Bold)
                            Text("Location: ${ctr.location}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Surface(
                            color = SuccessGreenLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("ACTIVE", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            // Gates Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Exit Verification Gates (${gates.size})", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                    TextButton(onClick = { showAddGateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Gate")
                    }
                }
            }

            items(gates) { gt ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${gt.gateId}: ${gt.name}", fontWeight = FontWeight.Bold)
                            Text("Location: ${gt.location}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Surface(
                            color = SuccessGreenLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("ACTIVE", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            // Staff PIN Reset List
            item {
                Text("Staff Accounts & PINs", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
            }

            items(users) { usr ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(usr.name, fontWeight = FontWeight.Bold)
                            Text("Role: ${usr.role} • ID: ${usr.userId} • Assigned: ${usr.assignedLocationId}", fontSize = 11.sp, color = Color.Gray)
                        }
                        TextButton(
                            onClick = { viewModel.repository.resetUserPin(usr.userId, "1234") }
                        ) {
                            Text("Reset PIN")
                        }
                    }
                }
            }
        }
    }

    if (showAddCounterDialog) {
        var name by remember { mutableStateOf("") }
        var loc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCounterDialog = false },
            title = { Text("Add New Billing Counter") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Counter Name (e.g. Counter 4)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location (e.g. 2nd Floor)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.repository.addCounter(name, loc)
                            showAddCounterDialog = false
                        }
                    }
                ) { Text("Create Counter") }
            },
            dismissButton = { TextButton(onClick = { showAddCounterDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddGateDialog) {
        var name by remember { mutableStateOf("") }
        var loc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddGateDialog = false },
            title = { Text("Add New Exit Gate") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Gate Name (e.g. Gate 3)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = loc, onValueChange = { loc = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.repository.addGate(name, loc)
                            showAddGateDialog = false
                        }
                    }
                ) { Text("Create Gate") }
            },
            dismissButton = { TextButton(onClick = { showAddGateDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ManagerReportsScreen(
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bills by viewModel.repository.bills.collectAsState()
    val settings by viewModel.repository.settings.collectAsState()

    val paidBills = bills.filter { it.paymentStatus == "PAID" }
    val totalSales = paidBills.sumOf { it.grandTotal }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Sales Reports & Analytics",
                department = Department.MANAGER,
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Daily Sales Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DeepNavyPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Total PAID Transactions: ${paidBills.size}", fontSize = 13.sp)
                    Text("Gross Store Turnover: ₹%.2f".format(totalSales), fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Export Actions (PDF and CSV)
            Button(
                onClick = {
                    val pdfFile = PdfReportGenerator.generateDailyReportPdf(context, bills, settings)
                    PdfReportGenerator.shareFile(context, pdfFile, "application/pdf", "Daily Sales Report")
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavyPrimary),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("export_sales_pdf_btn")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export & Share Daily Report (PDF)", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    // CSV share
                    val csv = buildString {
                        append("BillID,Date,Counter,Customer,PaymentMode,GrandTotal\n")
                        paidBills.forEach { b ->
                            append("${b.billId},${b.createdAt},${b.counterId},${b.customerPhone},${b.paymentMode},${b.grandTotal}\n")
                        }
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Sales Data CSV")
                        putExtra(Intent.EXTRA_TEXT, csv)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share CSV Summary"))
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Sales Data (CSV)")
            }
        }
    }
}

@Composable
fun ManagerAuditScreen(
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    val logs by viewModel.repository.auditLogs.collectAsState()

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Security Audit Logs",
                department = Department.MANAGER,
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
                .padding(14.dp)
        ) {
            Text("System & Staff Activity Trails (${logs.size}):", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No audit logs recorded yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(logs) { lg ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(lg.action, fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lg.timestamp))
                                    Text(timeStr, fontSize = 11.sp, color = Color.Gray)
                                }
                                Text("User: ${lg.userId} • Entity: ${lg.entity} (${lg.entityId})", fontSize = 12.sp, color = Color.DarkGray)
                                if (lg.newValue.isNotBlank()) {
                                    Text("Details: ${lg.newValue}", fontSize = 11.sp, color = TealAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
