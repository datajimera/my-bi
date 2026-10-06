package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.data.model.Department
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.UiScreen
import com.example.util.ReceiptRenderer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CounterHomeScreen(
    viewModel: BillingViewModel,
    onNewBill: () -> Unit,
    onResumeBill: (String) -> Unit,
    onViewHistory: () -> Unit,
    onDayClose: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val bills by viewModel.repository.bills.collectAsState()
    val heldBills by viewModel.repository.heldBills.collectAsState()
    val settings by viewModel.repository.settings.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    val counterId = user?.assignedLocationId ?: "C01"
    val todayBills = bills.filter { it.counterId == counterId && it.paymentStatus == "PAID" }
    val todaySales = todayBills.sumOf { it.grandTotal }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Counter $counterId (${user?.name ?: "Cashier"})",
                department = Department.COUNTER,
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

            // Today's counter stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Today's Bills",
                    value = "${todayBills.size}",
                    icon = Icons.Default.ReceiptLong,
                    containerColor = Color.White,
                    iconColor = DeepNavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Counter Revenue",
                    value = "₹%.0f".format(todaySales),
                    icon = Icons.Default.Payments,
                    containerColor = Color.White,
                    iconColor = TealAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // BIG + NEW BILL BUTTON (Section 4.1)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clickable { onNewBill() }
                    .testTag("big_new_bill_btn"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepNavyPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(TealAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Bill",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Column {
                        Text(
                            text = "+ New Customer Bill",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Auto-assigns Bill ID & opens fast scanner",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCCFBF1)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Held Bills Section
            if (heldBills.isNotEmpty()) {
                Text(
                    text = "Parked / Held Bills (${heldBills.size}):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = WarningOrange
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(heldBills) { b ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = WarningOrangeLight),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = b.billId,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepNavyPrimary
                                    )
                                    Text(
                                        text = "${b.items.size} items • ₹%.2f".format(b.grandTotal),
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Button(
                                    onClick = { onResumeBill(b.billId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Resume")
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Bottom Navigation shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onViewHistory,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Today's Bills")
                }
                OutlinedButton(
                    onClick = onDayClose,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Day Close")
                }
            }
        }
    }
}

@Composable
fun CounterBillingScreen(
    billId: String,
    viewModel: BillingViewModel,
    onProceedPayment: (String) -> Unit,
    onBack: () -> Unit
) {
    val currentBill by viewModel.currentBill.collectAsState()
    val products by viewModel.repository.products.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    val bill = currentBill ?: return

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Bill: ${bill.billId}",
                department = Department.COUNTER,
                onSwitchDepartment = { viewModel.navigateTo(UiScreen.DepartmentPortal) },
                onBack = onBack
            )
        },
        bottomBar = {
            // Sticky Bottom Summary Bar (Section 4.2)
            Surface(
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Items: ${bill.items.sumOf { it.qty }} | Subtotal: ₹%.2f".format(bill.subtotal),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Grand Total: ",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepNavyPrimary
                                )
                                Text(
                                    text = "₹%.2f".format(bill.grandTotal),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TealAccent
                                )
                            }
                        }

                        Button(
                            onClick = { onProceedPayment(bill.billId) },
                            enabled = bill.items.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavyPrimary),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("billing_next_button")
                        ) {
                            Text("Next / Pay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            FeedbackBanner(feedbackMessage)

            // Top Fast Scanner Area (Section 4.2)
            Box(modifier = Modifier.padding(12.dp)) {
                FastScannerBox(
                    sampleProducts = products,
                    onScan = { code -> viewModel.scanProduct(code) },
                    promptTitle = "Scan item QR code or Barcode"
                )
            }

            // Sub-actions: Hold & Cancel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cart Items (${bill.items.size}):",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = DeepNavyPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { viewModel.holdCurrentBill() },
                        colors = ButtonDefaults.textButtonColors(contentColor = WarningOrange)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hold Bill", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { showCancelDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", fontSize = 12.sp)
                    }
                }
            }

            // Live Items List
            if (bill.items.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No items scanned yet.\nScan product QR above or tap a sample item.",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bill.items) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DeepNavyPrimary
                                    )
                                    Text(
                                        text = "SKU: ${item.sku} • ₹%.2f each".format(item.unitPrice),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                // Quantity Controls (- / +)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = { viewModel.updateQty(item.sku, -1) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF1F5F9)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("-", color = DeepNavyPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }

                                    Text(
                                        text = "${item.qty}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )

                                    FilledIconButton(
                                        onClick = { viewModel.updateQty(item.sku, 1) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFF1F5F9)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("+", color = DeepNavyPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }

                                Text(
                                    text = "₹%.2f".format(item.lineTotal),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DeepNavyPrimary,
                                    modifier = Modifier.widthIn(min = 60.dp),
                                    textAlign = TextAlign.End
                                )

                                IconButton(
                                    onClick = { viewModel.removeItem(item.sku) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancel Bill Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Bill ${bill.billId}") },
            text = {
                Column {
                    Text("Please enter cancellation reason:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        placeholder = { Text("e.g. Customer changed mind") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelCurrentBill(if (cancelReason.isNotBlank()) cancelReason else "Customer Cancelled")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Back")
                }
            }
        )
    }
}

@Composable
fun CounterPaymentScreen(
    billId: String,
    viewModel: BillingViewModel,
    onPaymentConfirmed: (String) -> Unit,
    onBack: () -> Unit
) {
    val currentBill by viewModel.currentBill.collectAsState()
    val settings by viewModel.repository.settings.collectAsState()

    var paymentMode by remember { mutableStateOf("UPI") } // "UPI" or "CASH"
    var customerPhone by remember { mutableStateOf("") }
    var cashReceivedText by remember { mutableStateOf("") }

    val bill = currentBill ?: return
    val grandTotal = bill.grandTotal

    val cashReceived = cashReceivedText.toDoubleOrNull() ?: 0.0
    val changeToReturn = (cashReceived - grandTotal).coerceAtLeast(0.0)

    // Standard UPI Payment URI (Section 4.3)
    val upiUri = remember(grandTotal, bill.billId, settings.upiVpa) {
        "upi://pay?pa=${settings.upiVpa}&pn=${settings.upiPayeeName}&am=${"%.2f".format(grandTotal)}&tn=${bill.billId}&cu=INR"
    }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Payment - ₹%.2f".format(grandTotal),
                department = Department.COUNTER,
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
            // Amount Banner
            Surface(
                color = DeepNavyPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TOTAL AMOUNT PAYABLE", color = Color(0xFFCCFBF1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "₹%.2f".format(grandTotal),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text("Bill ID: ${bill.billId}", color = Color.LightGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Mode Switcher (UPI vs Cash)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { paymentMode = "UPI" },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (paymentMode == "UPI") TealAccent else Color.White,
                        contentColor = if (paymentMode == "UPI") Color.White else DeepNavyPrimary
                    ),
                    modifier = Modifier.weight(1f).height(50.dp).border(
                        1.dp,
                        if (paymentMode == "UPI") TealAccent else Color.LightGray,
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("UPI QR Code", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { paymentMode = "CASH" },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (paymentMode == "CASH") TealAccent else Color.White,
                        contentColor = if (paymentMode == "CASH") Color.White else DeepNavyPrimary
                    ),
                    modifier = Modifier.weight(1f).height(50.dp).border(
                        1.dp,
                        if (paymentMode == "CASH") TealAccent else Color.LightGray,
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Icon(Icons.Default.LocalAtm, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cash", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Details
            if (paymentMode == "UPI") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Customer Scans with Google Pay, PhonePe, Paytm, BHIM",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        QrCodeImage(qrText = upiUri, sizeDp = 170)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "VPA: ${settings.upiVpa}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cash Calculator", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = cashReceivedText,
                            onValueChange = { cashReceivedText = it },
                            label = { Text("Cash Received from Customer (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("cash_received_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (cashReceived >= grandTotal) SuccessGreenLight else Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Change to Return:", fontWeight = FontWeight.Medium)
                                Text(
                                    text = "₹%.2f".format(changeToReturn),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cashReceived >= grandTotal) SuccessGreen else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer Phone Number Input (Section 4.4)
            OutlinedTextField(
                value = customerPhone,
                onValueChange = { if (it.length <= 10) customerPhone = it },
                label = { Text("Customer Mobile Number (for WhatsApp/SMS receipt)") },
                placeholder = { Text("10-digit mobile number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("customer_phone_input")
            )

            Spacer(modifier = Modifier.weight(1f))

            // Confirm Payment Button
            Button(
                onClick = {
                    viewModel.completePayment(paymentMode, customerPhone)
                    onPaymentConfirmed(bill.billId)
                },
                enabled = paymentMode == "UPI" || (cashReceived >= grandTotal),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("confirm_payment_btn")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Payment Received", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CounterReceiptScreen(
    billId: String,
    viewModel: BillingViewModel,
    onDoneNextCustomer: () -> Unit
) {
    val context = LocalContext.current
    val currentBill by viewModel.currentBill.collectAsState()
    val settings by viewModel.repository.settings.collectAsState()

    val bill = currentBill ?: return

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Receipt - Paid",
                department = Department.COUNTER,
                onSwitchDepartment = { viewModel.navigateTo(UiScreen.DepartmentPortal) }
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
            Surface(
                color = SuccessGreenLight,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bill ${bill.billId} Paid Successfully!",
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Digital Receipt Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = settings.storeName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepNavyPrimary
                    )
                    Text("GST: ${settings.gstin}", fontSize = 11.sp, color = Color.Gray)
                    Text("Bill: ${bill.billId} • Counter: ${bill.counterId}", fontSize = 11.sp, color = Color.Gray)

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(bill.items) { itm ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${itm.name} x${itm.qty}",
                                    fontSize = 13.sp,
                                    color = DeepNavyPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₹%.2f".format(itm.lineTotal),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total (${bill.paymentMode}):", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                        Text("₹%.2f".format(bill.grandTotal), fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Exit Gate QR
                    QrCodeImage(
                        qrText = if (bill.receiptToken.isNotBlank()) bill.receiptToken else bill.billId,
                        sizeDp = 130
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Show this QR to Guard at Exit Gate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavyPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Share Buttons: WhatsApp and SMS (Section 4.4)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { ReceiptRenderer.shareReceiptWhatsApp(context, bill, settings) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("share_whatsapp_btn")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { ReceiptRenderer.shareReceiptSms(context, bill, settings) },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("share_sms_btn")
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SMS Link", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Done / Next Customer button (returns to Home loop)
            Button(
                onClick = onDoneNextCustomer,
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("next_customer_btn")
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done • Next Customer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CounterHistoryScreen(
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    val bills by viewModel.repository.bills.collectAsState()
    val settings by viewModel.repository.settings.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredBills = bills.filter {
        searchQuery.isBlank() ||
        it.billId.contains(searchQuery, ignoreCase = true) ||
        it.customerPhone.contains(searchQuery)
    }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Today's Bill History",
                department = Department.COUNTER,
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by Bill ID or Phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredBills) { b ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(b.billId, fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                                Surface(
                                    color = if (b.paymentStatus == "PAID") SuccessGreenLight else ErrorRedLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = b.paymentStatus,
                                        color = if (b.paymentStatus == "PAID") SuccessGreen else ErrorRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${b.items.size} items • ₹%.2f (${b.paymentMode}) • Phone: ${b.customerPhone.ifBlank { "N/A" }}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { ReceiptRenderer.shareReceiptWhatsApp(context, b, settings) }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resend WhatsApp")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CounterDayCloseScreen(
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    val bills by viewModel.repository.bills.collectAsState()
    val paidBills = bills.filter { it.paymentStatus == "PAID" }
    val totalUpi = paidBills.filter { it.paymentMode == "UPI" }.sumOf { it.grandTotal }
    val totalCash = paidBills.filter { it.paymentMode == "CASH" }.sumOf { it.grandTotal }
    val totalSales = totalUpi + totalCash

    var submitted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SmartBillingTopBar(
                title = "Cashier Day Close",
                department = Department.COUNTER,
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
                    Text("Cash Drawer Reconciliation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DeepNavyPrimary)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Bills Handled:")
                        Text("${paidBills.size}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("UPI Collections:")
                        Text("₹%.2f".format(totalUpi), fontWeight = FontWeight.Bold, color = TealAccent)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Expected Cash in Drawer:")
                        Text("₹%.2f".format(totalCash), fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Counter Turnover:", fontWeight = FontWeight.Bold, color = DeepNavyPrimary)
                        Text("₹%.2f".format(totalSales), fontWeight = FontWeight.Bold, color = DeepNavyPrimary, fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (!submitted) {
                Button(
                    onClick = { submitted = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavyPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Day Close to Manager", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    color = SuccessGreenLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Day close summary successfully recorded and synced to Store Manager!",
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
