package com.example.data.model

data class User(
    val userId: String,
    val name: String,
    val role: String, // "counter", "guard", "inventory", "manager"
    val pinHash: String,
    val assignedLocationId: String, // e.g. "C01" or "G01"
    val status: String = "active", // "active", "blocked"
    val failedAttempts: Int = 0,
    val lockedUntil: Long = 0L,
    val lastLogin: Long = 0L
)

data class Counter(
    val counterId: String,
    val name: String,
    val location: String,
    val status: String = "active"
)

data class Gate(
    val gateId: String,
    val name: String,
    val location: String,
    val status: String = "active"
)

data class Product(
    val sku: String,
    val name: String,
    val categoryId: String = "General",
    val mrp: Double,
    val sellPrice: Double,
    val costPrice: Double,
    val taxPercent: Double = 0.0,
    val unit: String = "pcs",
    val stockQty: Int,
    val reorderLevel: Int = 5,
    val status: String = "active",
    val barcode: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class BillItem(
    val billId: String,
    val sku: String,
    val name: String,
    var qty: Int,
    val unitPrice: Double,
    val taxPercent: Double = 0.0,
    val costPriceSnapshot: Double = 0.0
) {
    val lineTotal: Double
        get() = qty * unitPrice
}

data class Bill(
    val billId: String,
    val counterId: String,
    val cashierId: String,
    var customerPhone: String = "",
    var customerName: String = "",
    var subtotal: Double = 0.0,
    var discount: Double = 0.0,
    var taxTotal: Double = 0.0,
    var grandTotal: Double = 0.0,
    var paymentMode: String = "PENDING", // "UPI", "CASH", "SPLIT"
    var paymentStatus: String = "PENDING", // "PENDING", "PAID", "CANCELLED"
    var billStatus: String = "OPEN", // "OPEN", "HOLD", "COMPLETED", "CANCELLED"
    var receiptToken: String = "",
    var receiptLink: String = "",
    var whatsappSent: Boolean = false,
    var smsSent: Boolean = false,
    var checkedStatus: String = "NO", // "NO", "YES"
    var checkedBy: String = "",
    var checkedGateId: String = "",
    var checkedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    var paidAt: Long = 0L,
    var cancelReason: String = "",
    var items: List<BillItem> = emptyList()
)

data class StockLog(
    val logId: String,
    val sku: String,
    val changeQty: Int,
    val reason: String, // "SALE", "PURCHASE", "ADJUST", "RETURN"
    val refId: String,
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AuditLog(
    val logId: String,
    val userId: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val oldValue: String = "",
    val newValue: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class GateScanRecord(
    val scanId: String,
    val billId: String,
    val guardId: String,
    val gateId: String,
    val result: String, // "APPROVED", "ALREADY_CHECKED", "NOT_PAID", "INVALID_QR"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

data class StoreSettings(
    val storeName: String = "Smart Supermarket",
    val address: String = "Main Street Retail Hub, City Center",
    val phone: String = "+91 98765 43210",
    val gstin: String = "27AAAAA0000A1Z5",
    val upiVpa: String = "smartbilling@upi",
    val upiPayeeName: String = "Smart Supermarket Billing",
    val receiptFooter: String = "Thank you for shopping with us! Please verify at exit gate.",
    val currency: String = "₹",
    val taxMode: String = "Inclusive",
    val appsScriptUrl: String = "",
    val apiKey: String = "SB-KEY-2026-X99",
    val hmacSecret: String = "SecretKeyBilling2026Salted",
    val duplicateScanGuardTimeSeconds: Double = 1.2,
    val allowSellOutOfStock: Boolean = false
)

enum class Department(val displayName: String, val roleId: String, val defaultPin: String, val description: String) {
    COUNTER("Billing Counter", "counter", "1234", "Fast QR checkout, UPI & Cash bills, WhatsApp receipts"),
    GUARD("Exit Guard", "guard", "4321", "Receipt QR verification, bag items check, anti-theft exit"),
    INVENTORY("Inventory Management", "inventory", "5678", "Stock tracking, new products, 10-per-A4 QR sheets"),
    MANAGER("Store Manager", "manager", "9999", "Live sales & profit dashboard, staff control, script setup")
}
