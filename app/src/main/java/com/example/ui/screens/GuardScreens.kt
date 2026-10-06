package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.data.model.Department
import com.example.ui.components.FastScannerBox
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.SmartBillingTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.UiScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GuardScanScreen(
    viewModel: BillingViewModel,
    onViewHistory: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val bills by viewModel.repository.bills.collectAsState()
    val gateScans by viewModel.repository.gateScans.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    var manualBillId by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }

    val gateId = user?.assignedLocationId ?: "G01"
    val guardName = user?.name ?: "Security Guard"

    val approvedCount = gateScans.count { it.result == "APPROVED" }
    val duplicateCount = gateScans.count { it.result == "ALREADY_CHECKED" }
    val invalidCount = gateScans.count { it.result == "INVALID_QR" || it.result == "NOT_PAID" }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Exit Gate $gateId ($guardName)",
                department = Department.GUARD,
                onSwitchDepartment = { viewModel.navigateTo(UiScreen.DepartmentPortal) }
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
            FeedbackBanner(feedbackMessage)

            // Guard Shift Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SuccessGreenLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Approved", fontSize = 11.sp, color = SuccessGreen)
                        Text("$approvedCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                }
                Surface(
                    color = WarningOrangeLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duplicate", fontSize = 11.sp, color = WarningOrange)
                        Text("$duplicateCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                    }
                }
                Surface(
                    color = ErrorRedLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Rejected", fontSize = 11.sp, color = ErrorRed)
                        Text("$invalidCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Full-screen continuous receipt scanner (Section 5)
            FastScannerBox(
                onScan = { qrContent -> viewModel.verifyGateExit(qrContent) },
                promptTitle = "Scan Customer's Receipt QR code",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Quick One-Tap Test Bills (allows testing approved, duplicate, not paid states easily in emulator)
            Text(
                text = "Tap a recent customer receipt to test verification:",
                fontSize = 12.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(bills.take(4)) { b ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                        modifier = Modifier.testTag("guard_test_bill_${b.billId}")
                    ) {
                        Button(
                            onClick = { viewModel.verifyGateExit(b.receiptToken.ifBlank { b.billId }) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = DeepNavyPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text(b.billId, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Status: ${if (b.checkedStatus == "YES") "Checked" else b.paymentStatus}",
                                    fontSize = 10.sp,
                                    color = if (b.checkedStatus == "YES") WarningOrange else SuccessGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Manual Entry & History Buttons (Section 5 & 5.2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showManualDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("guard_manual_entry_btn")
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual Entry (SMS)")
                }

                Button(
                    onClick = onViewHistory,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyPrimary),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gate History")
                }
            }
        }
    }

    if (showManualDialog) {
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Manual Bill ID Entry") },
            text = {
                Column {
                    Text("Enter Bill ID from customer's SMS receipt (e.g. C01-20261006-0001):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualBillId,
                        onValueChange = { manualBillId = it },
                        placeholder = { Text("Type Bill ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualBillId.isNotBlank()) {
                            showManualDialog = false
                            viewModel.verifyGateExit(manualBillId.trim())
                            manualBillId = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyPrimary)
                ) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GuardResultScreen(
    status: String,
    message: String,
    bill: Bill?,
    onScanNext: () -> Unit
) {
    val (bgColor, icon, titleText) = when (status) {
        "APPROVED" -> Triple(SuccessGreen, Icons.Default.CheckCircle, "APPROVED - EXIT ALLOWED")
        "ALREADY_CHECKED" -> Triple(WarningOrange, Icons.Default.Warning, "ALREADY CHECKED - DUPLICATE SCAN")
        "NOT_PAID" -> Triple(ErrorRed, Icons.Default.Cancel, "PAYMENT PENDING - DO NOT EXIT")
        else -> Triple(ErrorRed, Icons.Default.ErrorOutline, "INVALID QR / BILL NOT FOUND")
    }

    var showReportDialog by remember { mutableStateOf(false) }
    var reportNote by remember { mutableStateOf("") }
    var reportSubmitted by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Big Result Icon & Status Header (Section 5.2 & Section 12)
            Icon(
                imageVector = icon,
                contentDescription = status,
                tint = Color.White,
                modifier = Modifier.size(76.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Purchased Item List for physical verification (Section 5.2: "guard sees the purchased item list below the green banner so they can match the physical bag with the screen quickly")
            if (bill != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Bill: ${bill.billId}",
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavyPrimary
                                )
                                Text(
                                    text = "Counter: ${bill.counterId} • Paid via ${bill.paymentMode}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "₹%.2f".format(bill.grandTotal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = DeepNavyPrimary
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))

                        Text(
                            text = "CHECK BAG ITEMS (${bill.items.sumOf { it.qty }} TOTAL):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(bill.items) { itm ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = itm.name,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = DeepNavyPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = DeepNavyPrimary,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "QTY: ${itm.qty}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional Report Issue / Mismatch Button
            if (bill != null && !reportSubmitted) {
                TextButton(
                    onClick = { showReportDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Report Bag / Item Mismatch to Manager", textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                }
            } else if (reportSubmitted) {
                Text(
                    text = "✓ Item mismatch noted and flagged to Manager",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Big Next Customer Scan Button
            Button(
                onClick = onScanNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepNavyPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("guard_scan_next_btn")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Next Customer Receipt", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Item Mismatch") },
            text = {
                Column {
                    Text("Describe items in bag that did not match the bill:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportNote,
                        onValueChange = { reportNote = it },
                        placeholder = { Text("e.g. Found unbilled chocolate in bag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReportDialog = false
                        reportSubmitted = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Send Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GuardHistoryScreen(
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    val gateScans by viewModel.repository.gateScans.collectAsState()

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Guard Scan History",
                department = Department.GUARD,
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
            Text(
                text = "Today's Verified & Checked Receipts (${gateScans.size}):",
                fontWeight = FontWeight.Bold,
                color = DeepNavyPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (gateScans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No scans recorded yet today.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(gateScans) { scan ->
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
                                    Text(scan.billId, fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                                    val timeStr = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(scan.timestamp))
                                    Text("Gate: ${scan.gateId} • Guard: ${scan.guardId} • $timeStr", fontSize = 11.sp, color = Color.Gray)
                                    if (scan.notes.isNotBlank()) {
                                        Text(scan.notes, fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }
                                Surface(
                                    color = when (scan.result) {
                                        "APPROVED" -> SuccessGreenLight
                                        "ALREADY_CHECKED" -> WarningOrangeLight
                                        else -> ErrorRedLight
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = scan.result,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (scan.result) {
                                            "APPROVED" -> SuccessGreen
                                            "ALREADY_CHECKED" -> WarningOrange
                                            else -> ErrorRed
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
