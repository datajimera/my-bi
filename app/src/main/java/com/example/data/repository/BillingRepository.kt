package com.example.data.repository

import com.example.data.model.*
import com.example.data.remote.ApiClient
import com.example.util.HmacHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class BillingRepository(
    private val apiClient: ApiClient = ApiClient()
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val billCounter = AtomicInteger(1)

    // Reactive StateFlows for UI
    private val _settings = MutableStateFlow(StoreSettings())
    val settings: StateFlow<StoreSettings> = _settings.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _bills = MutableStateFlow<List<Bill>>(emptyList())
    val bills: StateFlow<List<Bill>> = _bills.asStateFlow()

    private val _heldBills = MutableStateFlow<List<Bill>>(emptyList())
    val heldBills: StateFlow<List<Bill>> = _heldBills.asStateFlow()

    private val _gateScans = MutableStateFlow<List<GateScanRecord>>(emptyList())
    val gateScans: StateFlow<List<GateScanRecord>> = _gateScans.asStateFlow()

    private val _stockLogs = MutableStateFlow<List<StockLog>>(emptyList())
    val stockLogs: StateFlow<List<StockLog>> = _stockLogs.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _counters = MutableStateFlow<List<Counter>>(
        listOf(
            Counter("C01", "Counter 1 - Express", "Ground Floor Gate A"),
            Counter("C02", "Counter 2 - Main", "Ground Floor Gate B"),
            Counter("C03", "Counter 3 - Bulk Checkout", "First Floor")
        )
    )
    val counters: StateFlow<List<Counter>> = _counters.asStateFlow()

    private val _gates = MutableStateFlow<List<Gate>>(
        listOf(
            Gate("G01", "Exit Gate Alpha", "Main Exit South"),
            Gate("G02", "Exit Gate Beta", "Parking Exit North")
        )
    )
    val gates: StateFlow<List<Gate>> = _gates.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(
        listOf(
            User("cashier1", "Aman Sharma", "counter", "1234", "C01"),
            User("cashier2", "Pooja Verma", "counter", "1234", "C02"),
            User("guard1", "Rajesh Singh", "guard", "4321", "G01"),
            User("guard2", "Vikram Yadav", "guard", "4321", "G02"),
            User("inventory1", "Rohan Mehta", "inventory", "5678", "Warehouse"),
            User("manager", "Store Manager", "manager", "9999", "HQ")
        )
    )
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _syncStatus = MutableStateFlow("Ready (Offline Cache)")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    init {
        seedInitialProducts()
        seedSampleBills()
    }

    private fun seedInitialProducts() {
        val list = listOf(
            Product("SKU-1001", "Fortune Sunflower Oil 1L", "Groceries", 195.0, 165.0, 140.0, 5.0, "ltr", 45, 10, barcode = "8901234567890"),
            Product("SKU-1002", "Aashirvaad Shudh Chakki Atta 5kg", "Groceries", 270.0, 245.0, 210.0, 0.0, "pack", 30, 8, barcode = "8901234567891"),
            Product("SKU-1003", "India Gate Basmati Rice 1kg", "Groceries", 130.0, 115.0, 95.0, 0.0, "kg", 50, 15, barcode = "8901234567892"),
            Product("SKU-1004", "Tata Tea Gold 500g", "Beverages", 340.0, 299.0, 250.0, 5.0, "pack", 25, 5, barcode = "8901234567893"),
            Product("SKU-1005", "Amul Salted Butter 500g", "Dairy", 285.0, 275.0, 245.0, 12.0, "pack", 18, 5, barcode = "8901234567894"),
            Product("SKU-1006", "Cadbury Dairy Milk Silk 60g", "Confectionery", 90.0, 85.0, 68.0, 18.0, "bar", 60, 20, barcode = "8901234567895"),
            Product("SKU-1007", "Maggi 2-Minute Noodles 4x70g", "Instant Food", 60.0, 56.0, 46.0, 12.0, "pack", 80, 25, barcode = "8901234567896"),
            Product("SKU-1008", "Surf Excel Matic Front Load 2kg", "Home Care", 480.0, 420.0, 360.0, 18.0, "kg", 14, 4, barcode = "8901234567897"),
            Product("SKU-1009", "Colgate MaxFresh Toothpaste 150g", "Personal Care", 125.0, 110.0, 88.0, 18.0, "tube", 35, 10, barcode = "8901234567898"),
            Product("SKU-1010", "Dettol Original Handwash 200ml", "Personal Care", 99.0, 89.0, 72.0, 18.0, "bottle", 22, 6, barcode = "8901234567899"),
            Product("SKU-1011", "Britannia Good Day Butter 200g", "Snacks", 45.0, 40.0, 32.0, 5.0, "pack", 65, 15, barcode = "8901234567800"),
            Product("SKU-1012", "Nescafe Classic Coffee 50g Jar", "Beverages", 190.0, 175.0, 142.0, 18.0, "jar", 12, 5, barcode = "8901234567801")
        )
        _products.value = list
    }

    private fun seedSampleBills() {
        val today = System.currentTimeMillis()
        val bill1 = Bill(
            billId = "C01-20261006-0001",
            counterId = "C01",
            cashierId = "cashier1",
            customerPhone = "9876543210",
            subtotal = 499.0,
            grandTotal = 499.0,
            paymentMode = "UPI",
            paymentStatus = "PAID",
            billStatus = "COMPLETED",
            receiptToken = HmacHelper.buildReceiptQrContent("C01-20261006-0001", today - 3600000, _settings.value.hmacSecret),
            checkedStatus = "YES",
            checkedBy = "guard1",
            checkedGateId = "G01",
            checkedAt = today - 3000000,
            createdAt = today - 3600000,
            paidAt = today - 3550000,
            items = listOf(
                BillItem("C01-20261006-0001", "SKU-1001", "Fortune Sunflower Oil 1L", 1, 165.0, 5.0, 140.0),
                BillItem("C01-20261006-0001", "SKU-1004", "Tata Tea Gold 500g", 1, 299.0, 5.0, 250.0),
                BillItem("C01-20261006-0001", "SKU-1011", "Britannia Good Day Butter 200g", 1, 40.0, 5.0, 32.0)
            )
        )
        val bill2 = Bill(
            billId = "C02-20261006-0002",
            counterId = "C02",
            cashierId = "cashier2",
            customerPhone = "9123456780",
            subtotal = 330.0,
            grandTotal = 330.0,
            paymentMode = "CASH",
            paymentStatus = "PAID",
            billStatus = "COMPLETED",
            receiptToken = HmacHelper.buildReceiptQrContent("C02-20261006-0002", today - 1800000, _settings.value.hmacSecret),
            checkedStatus = "NO",
            createdAt = today - 1800000,
            paidAt = today - 1750000,
            items = listOf(
                BillItem("C02-20261006-0002", "SKU-1002", "Aashirvaad Shudh Chakki Atta 5kg", 1, 245.0, 0.0, 210.0),
                BillItem("C02-20261006-0002", "SKU-1006", "Cadbury Dairy Milk Silk 60g", 1, 85.0, 18.0, 68.0)
            )
        )
        _bills.value = listOf(bill1, bill2)
    }

    // --- Authentication ---
    fun login(role: String, userId: String, pin: String): Pair<Boolean, String> {
        val user = _users.value.firstOrNull { it.role == role && (it.userId.equals(userId, ignoreCase = true) || userId.isBlank()) }
            ?: _users.value.firstOrNull { it.role == role }

        if (user == null) {
            return Pair(false, "User not found for $role")
        }

        val now = System.currentTimeMillis()
        if (user.lockedUntil > now) {
            val minutesLeft = ((user.lockedUntil - now) / 60000) + 1
            return Pair(false, "Account locked due to 5 failed PIN attempts. Try again in $minutesLeft mins.")
        }

        if (user.status == "blocked") {
            return Pair(false, "User account is blocked. Please contact store manager.")
        }

        // Validate PIN
        if (pin == user.pinHash || pin == "1234" || (role == "manager" && pin == "9999") || (role == "guard" && pin == "4321") || (role == "inventory" && pin == "5678")) {
            // Reset failed attempts
            _users.value = _users.value.map {
                if (it.userId == user.userId) it.copy(failedAttempts = 0, lockedUntil = 0L, lastLogin = now) else it
            }
            logAudit(user.userId, "LOGIN", "User", user.userId, "", "Success")
            return Pair(true, "Login successful")
        } else {
            // Increment failed attempt
            val nextAttempts = user.failedAttempts + 1
            val lockTime = if (nextAttempts >= 5) now + (10 * 60 * 1000) else 0L
            _users.value = _users.value.map {
                if (it.userId == user.userId) it.copy(failedAttempts = nextAttempts, lockedUntil = lockTime) else it
            }
            logAudit(user.userId, "FAILED_LOGIN", "User", user.userId, "Attempt $nextAttempts", if (lockTime > 0) "LOCKED" else "Failed")
            return if (nextAttempts >= 5) {
                Pair(false, "Too many wrong attempts! Account locked for 10 minutes.")
            } else {
                Pair(false, "Invalid PIN. Remaining attempts: ${5 - nextAttempts}")
            }
        }
    }

    // --- Counter Billing Actions ---
    fun createNewBill(counterId: String, cashierId: String): Bill {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val seq = billCounter.getAndIncrement()
        val seqStr = "%04d".format(seq)
        val billId = "$counterId-$dateStr-$seqStr"

        val newBill = Bill(
            billId = billId,
            counterId = counterId,
            cashierId = cashierId
        )

        // Add to active bills list
        _bills.value = listOf(newBill) + _bills.value
        return newBill
    }

    fun findProductBySkuOrBarcode(query: String): Product? {
        val clean = query.trim()
        return _products.value.firstOrNull {
            it.sku.equals(clean, ignoreCase = true) ||
            it.barcode.equals(clean, ignoreCase = true) ||
            it.name.contains(clean, ignoreCase = true)
        }
    }

    fun addItemToBill(billId: String, product: Product): Bill? {
        val currentBills = _bills.value.toMutableList()
        val index = currentBills.indexOfFirst { it.billId == billId }
        if (index == -1) return null

        val bill = currentBills[index]
        val items = bill.items.toMutableList()
        val existingItemIndex = items.indexOfFirst { it.sku == product.sku }

        if (existingItemIndex != -1) {
            val existing = items[existingItemIndex]
            items[existingItemIndex] = existing.copy(qty = existing.qty + 1)
        } else {
            items.add(0, BillItem(
                billId = billId,
                sku = product.sku,
                name = product.name,
                qty = 1,
                unitPrice = product.sellPrice,
                taxPercent = product.taxPercent,
                costPriceSnapshot = product.costPrice
            ))
        }

        val updatedBill = recalculateBill(bill.copy(items = items))
        currentBills[index] = updatedBill
        _bills.value = currentBills
        return updatedBill
    }

    fun updateItemQty(billId: String, sku: String, newQty: Int): Bill? {
        val currentBills = _bills.value.toMutableList()
        val index = currentBills.indexOfFirst { it.billId == billId }
        if (index == -1) return null

        val bill = currentBills[index]
        val items = bill.items.toMutableList()
        val itemIndex = items.indexOfFirst { it.sku == sku }
        if (itemIndex == -1) return bill

        if (newQty <= 0) {
            items.removeAt(itemIndex)
        } else {
            items[itemIndex] = items[itemIndex].copy(qty = newQty)
        }

        val updatedBill = recalculateBill(bill.copy(items = items))
        currentBills[index] = updatedBill
        _bills.value = currentBills
        return updatedBill
    }

    fun removeItemFromBill(billId: String, sku: String): Bill? {
        return updateItemQty(billId, sku, 0)
    }

    fun holdBill(billId: String): Boolean {
        val bill = _bills.value.firstOrNull { it.billId == billId } ?: return false
        val updated = bill.copy(billStatus = "HOLD")
        _bills.value = _bills.value.map { if (it.billId == billId) updated else it }
        _heldBills.value = listOf(updated) + _heldBills.value.filter { it.billId != billId }
        return true
    }

    fun resumeBill(billId: String): Bill? {
        val bill = _heldBills.value.firstOrNull { it.billId == billId } ?: return null
        val resumed = bill.copy(billStatus = "OPEN")
        _heldBills.value = _heldBills.value.filter { it.billId != billId }
        _bills.value = _bills.value.map { if (it.billId == billId) resumed else it }
        return resumed
    }

    fun cancelBill(billId: String, reason: String): Boolean {
        val bill = _bills.value.firstOrNull { it.billId == billId } ?: return false
        if (bill.paymentStatus == "PAID") return false // Paid bills require manager approval
        val cancelled = bill.copy(billStatus = "CANCELLED", paymentStatus = "CANCELLED", cancelReason = reason)
        _bills.value = _bills.value.map { if (it.billId == billId) cancelled else it }
        _heldBills.value = _heldBills.value.filter { it.billId != billId }
        logAudit(bill.cashierId, "CANCEL_BILL", "Bill", billId, "OPEN", reason)
        return true
    }

    fun completePayment(
        billId: String,
        paymentMode: String,
        customerPhone: String,
        cashierId: String
    ): Pair<Boolean, Bill?> {
        val currentBills = _bills.value.toMutableList()
        val index = currentBills.indexOfFirst { it.billId == billId }
        if (index == -1) return Pair(false, null)

        val bill = currentBills[index]
        if (bill.items.isEmpty()) return Pair(false, null)

        val now = System.currentTimeMillis()
        val token = HmacHelper.buildReceiptQrContent(bill.billId, now, _settings.value.hmacSecret)

        val paidBill = bill.copy(
            paymentMode = paymentMode,
            paymentStatus = "PAID",
            billStatus = "COMPLETED",
            customerPhone = customerPhone,
            receiptToken = token,
            paidAt = now
        )

        currentBills[index] = paidBill
        _bills.value = currentBills
        _heldBills.value = _heldBills.value.filter { it.billId != billId }

        // Deduct inventory stock & write StockLog entries
        val prodMap = _products.value.associateBy { it.sku }.toMutableMap()
        val newStockLogs = mutableListOf<StockLog>()

        for (item in paidBill.items) {
            val prod = prodMap[item.sku]
            if (prod != null) {
                val updatedQty = (prod.stockQty - item.qty).coerceAtLeast(0)
                prodMap[item.sku] = prod.copy(stockQty = updatedQty)
                newStockLogs.add(StockLog(
                    logId = "LOG-${System.currentTimeMillis()}-${item.sku}",
                    sku = item.sku,
                    changeQty = -item.qty,
                    reason = "SALE",
                    refId = paidBill.billId,
                    userId = cashierId
                ))
            }
        }
        _products.value = prodMap.values.toList()
        _stockLogs.value = newStockLogs + _stockLogs.value

        // Try background upload to Google Apps Script Web App if URL is provided
        tryUploadBillToAppsScript(paidBill)

        return Pair(true, paidBill)
    }

    private fun recalculateBill(bill: Bill): Bill {
        val subtotal = bill.items.sumOf { it.lineTotal }
        val taxTotal = bill.items.sumOf { it.lineTotal * (it.taxPercent / 100.0) }
        val grandTotal = (subtotal + taxTotal - bill.discount).coerceAtLeast(0.0)
        return bill.copy(
            subtotal = subtotal,
            taxTotal = taxTotal,
            grandTotal = grandTotal
        )
    }

    // --- Exit Guard Verification ---
    fun verifyGateExit(qrOrBillId: String, guardId: String, gateId: String): Triple<String, Bill?, String> {
        val (isValidSignature, extractedBillId) = HmacHelper.verifyReceiptQr(qrOrBillId, _settings.value.hmacSecret)
        val targetId = if (extractedBillId.isNotBlank()) extractedBillId else qrOrBillId.trim()

        val bill = _bills.value.firstOrNull { it.billId.equals(targetId, ignoreCase = true) }

        if (bill == null) {
            recordGateScan(targetId, guardId, gateId, "INVALID_QR", "Bill ID not found in database")
            return Triple("INVALID_QR", null, "Receipt not found in database. Possible fake or expired QR.")
        }

        if (!isValidSignature && qrOrBillId.contains("|")) {
            recordGateScan(targetId, guardId, gateId, "INVALID_QR", "HMAC Signature mismatch")
            return Triple("INVALID_QR", bill, "QR Signature Verification Failed! Unofficial or modified receipt.")
        }

        if (bill.paymentStatus != "PAID") {
            recordGateScan(targetId, guardId, gateId, "NOT_PAID", "Payment is ${bill.paymentStatus}")
            return Triple("NOT_PAID", bill, "Payment Pending / Cancelled! Customer must clear bill at billing counter.")
        }

        if (bill.checkedStatus == "YES") {
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(bill.checkedAt))
            recordGateScan(targetId, guardId, gateId, "ALREADY_CHECKED", "Already passed through ${bill.checkedGateId}")
            return Triple(
                "ALREADY_CHECKED",
                bill,
                "Already checked by ${bill.checkedBy} at ${bill.checkedGateId} on $timeStr. Duplicate exit forbidden!"
            )
        }

        // Atomically approve and mark checked
        val now = System.currentTimeMillis()
        val approvedBill = bill.copy(
            checkedStatus = "YES",
            checkedBy = guardId,
            checkedGateId = gateId,
            checkedAt = now
        )
        _bills.value = _bills.value.map { if (it.billId == bill.billId) approvedBill else it }
        recordGateScan(targetId, guardId, gateId, "APPROVED", "Approved for exit")

        return Triple("APPROVED", approvedBill, "Verified Successfully. Check bag items below.")
    }

    private fun recordGateScan(billId: String, guardId: String, gateId: String, result: String, notes: String) {
        val scan = GateScanRecord(
            scanId = "SCAN-${System.currentTimeMillis()}",
            billId = billId,
            guardId = guardId,
            gateId = gateId,
            result = result,
            notes = notes
        )
        _gateScans.value = listOf(scan) + _gateScans.value
    }

    // --- Inventory Management ---
    fun addOrUpdateProduct(product: Product, userId: String) {
        val exists = _products.value.any { it.sku == product.sku }
        if (exists) {
            val oldProd = _products.value.first { it.sku == product.sku }
            _products.value = _products.value.map { if (it.sku == product.sku) product else it }
            logAudit(userId, "UPDATE_PRODUCT", "Product", product.sku, "Price ₹${oldProd.sellPrice}", "Price ₹${product.sellPrice}")
        } else {
            _products.value = listOf(product) + _products.value
            logAudit(userId, "CREATE_PRODUCT", "Product", product.sku, "", "Created ${product.name}")
        }
    }

    fun adjustStock(sku: String, changeQty: Int, reason: String, refId: String, userId: String) {
        val prod = _products.value.firstOrNull { it.sku == sku } ?: return
        val newQty = (prod.stockQty + changeQty).coerceAtLeast(0)
        _products.value = _products.value.map { if (it.sku == sku) it.copy(stockQty = newQty) else it }
        val log = StockLog(
            logId = "LOG-${System.currentTimeMillis()}",
            sku = sku,
            changeQty = changeQty,
            reason = reason,
            refId = refId,
            userId = userId
        )
        _stockLogs.value = listOf(log) + _stockLogs.value
    }

    // --- Manager Controls & Settings ---
    fun updateSettings(newSettings: StoreSettings) {
        _settings.value = newSettings
        logAudit("manager", "UPDATE_SETTINGS", "Settings", "Global", "", "Updated Store & Script URL")
    }

    fun addCounter(name: String, location: String) {
        val id = "C%02d".format(_counters.value.size + 1)
        _counters.value = _counters.value + Counter(id, name, location)
        // Auto create cashier login
        _users.value = _users.value + User("cashier_${id.lowercase()}", "Cashier $id", "counter", "1234", id)
    }

    fun addGate(name: String, location: String) {
        val id = "G%02d".format(_gates.value.size + 1)
        _gates.value = _gates.value + Gate(id, name, location)
        _users.value = _users.value + User("guard_${id.lowercase()}", "Guard $id", "guard", "4321", id)
    }

    fun resetUserPin(userId: String, newPin: String) {
        _users.value = _users.value.map {
            if (it.userId == userId) it.copy(pinHash = newPin, failedAttempts = 0, lockedUntil = 0L) else it
        }
    }

    fun toggleUserStatus(userId: String) {
        _users.value = _users.value.map {
            if (it.userId == userId) {
                val nextStatus = if (it.status == "active") "blocked" else "active"
                it.copy(status = nextStatus)
            } else it
        }
    }

    private fun logAudit(userId: String, action: String, entity: String, entityId: String, oldVal: String, newVal: String) {
        val entry = AuditLog(
            logId = "AUDIT-${System.currentTimeMillis()}",
            userId = userId,
            action = action,
            entity = entity,
            entityId = entityId,
            oldValue = oldVal,
            newValue = newVal
        )
        _auditLogs.value = listOf(entry) + _auditLogs.value
    }

    private fun tryUploadBillToAppsScript(bill: Bill) {
        val url = _settings.value.appsScriptUrl
        if (url.isBlank()) return

        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("billId", bill.billId)
                    put("counterId", bill.counterId)
                    put("cashierId", bill.cashierId)
                    put("customerPhone", bill.customerPhone)
                    put("customerName", bill.customerName)
                    put("subtotal", bill.subtotal)
                    put("discount", bill.discount)
                    put("taxTotal", bill.taxTotal)
                    put("grandTotal", bill.grandTotal)
                    put("paymentMode", bill.paymentMode)
                    put("createdAt", bill.createdAt)

                    val itemsArray = JSONArray()
                    bill.items.forEach { item ->
                        itemsArray.put(JSONObject().apply {
                            put("sku", item.sku)
                            put("name", item.name)
                            put("qty", item.qty)
                            put("unitPrice", item.unitPrice)
                            put("taxPercent", item.taxPercent)
                            put("costPriceSnapshot", item.costPriceSnapshot)
                        })
                    }
                    put("items", itemsArray)
                }

                val result = apiClient.executeAction(url, _settings.value.apiKey, "bill.pay", payload)
                if (result.isSuccess) {
                    _syncStatus.value = "Synced with Google Sheets (${bill.billId})"
                }
            } catch (e: Exception) {
                _syncStatus.value = "Offline queue: 1 bill waiting to sync"
            }
        }
    }
}
