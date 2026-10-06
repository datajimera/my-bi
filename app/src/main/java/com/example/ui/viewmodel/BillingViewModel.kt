package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.ApiClient
import com.example.data.repository.BillingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiScreen {
    object DepartmentPortal : UiScreen()
    data class Login(val department: Department) : UiScreen()
    
    // Counter Flow
    object CounterHome : UiScreen()
    data class CounterBilling(val billId: String) : UiScreen()
    data class CounterPayment(val billId: String) : UiScreen()
    data class CounterReceipt(val billId: String) : UiScreen()
    object CounterHistory : UiScreen()
    object CounterDayClose : UiScreen()

    // Guard Flow
    object GuardScan : UiScreen()
    data class GuardResult(val status: String, val message: String, val bill: Bill?) : UiScreen()
    object GuardHistory : UiScreen()

    // Inventory Flow
    object InventoryHome : UiScreen()
    object InventoryQrPrint : UiScreen()
    object InventoryAnalytics : UiScreen()

    // Manager Flow
    object ManagerDashboard : UiScreen()
    object ManagerStaff : UiScreen()
    object ManagerSettings : UiScreen()
    object ManagerReports : UiScreen()
    object ManagerAudit : UiScreen()
}

class BillingViewModel(
    val repository: BillingRepository = BillingRepository()
) : ViewModel() {

    private val apiClient = ApiClient()

    private val _currentScreen = MutableStateFlow<UiScreen>(UiScreen.DepartmentPortal)
    val currentScreen: StateFlow<UiScreen> = _currentScreen.asStateFlow()

    private val _activeDepartment = MutableStateFlow(Department.COUNTER)
    val activeDepartment: StateFlow<Department> = _activeDepartment.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentBill = MutableStateFlow<Bill?>(null)
    val currentBill: StateFlow<Bill?> = _currentBill.asStateFlow()

    // Banner message for scan feedback / alerts
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()

    fun navigateTo(screen: UiScreen) {
        _currentScreen.value = screen
    }

    fun selectDepartment(dept: Department) {
        _activeDepartment.value = dept
        navigateTo(UiScreen.Login(dept))
    }

    fun login(pin: String, onResult: (Boolean, String) -> Unit) {
        val dept = _activeDepartment.value
        val (success, message) = repository.login(dept.roleId, "", pin)
        if (success) {
            val user = repository.users.value.firstOrNull { it.role == dept.roleId }
                ?: User("staff", "Staff ${dept.displayName}", dept.roleId, pin, "LOC1")
            _currentUser.value = user
            when (dept) {
                Department.COUNTER -> navigateTo(UiScreen.CounterHome)
                Department.GUARD -> navigateTo(UiScreen.GuardScan)
                Department.INVENTORY -> navigateTo(UiScreen.InventoryHome)
                Department.MANAGER -> navigateTo(UiScreen.ManagerDashboard)
            }
        }
        onResult(success, message)
    }

    fun logout() {
        _currentUser.value = null
        navigateTo(UiScreen.DepartmentPortal)
    }

    // --- Counter Billing Actions ---
    fun startNewBill() {
        val user = _currentUser.value
        val counterId = user?.assignedLocationId?.takeIf { it.isNotBlank() } ?: "C01"
        val cashierId = user?.userId ?: "cashier"
        val newBill = repository.createNewBill(counterId, cashierId)
        _currentBill.value = newBill
        navigateTo(UiScreen.CounterBilling(newBill.billId))
        showFeedback("New Bill ${newBill.billId} started")
    }

    fun scanProduct(query: String) {
        val bill = _currentBill.value ?: return
        val product = repository.findProductBySkuOrBarcode(query)
        if (product != null) {
            val updated = repository.addItemToBill(bill.billId, product)
            _currentBill.value = updated
            showFeedback("Added: ${product.name} (₹${product.sellPrice})")
        } else {
            showFeedback("Product not found: $query", isError = true)
        }
    }

    fun updateQty(sku: String, delta: Int) {
        val bill = _currentBill.value ?: return
        val currentItem = bill.items.firstOrNull { it.sku == sku } ?: return
        val newQty = currentItem.qty + delta
        val updated = repository.updateItemQty(bill.billId, sku, newQty)
        _currentBill.value = updated
    }

    fun removeItem(sku: String) {
        val bill = _currentBill.value ?: return
        val updated = repository.removeItemFromBill(bill.billId, sku)
        _currentBill.value = updated
    }

    fun holdCurrentBill() {
        val bill = _currentBill.value ?: return
        repository.holdBill(bill.billId)
        _currentBill.value = null
        navigateTo(UiScreen.CounterHome)
        showFeedback("Bill ${bill.billId} placed on Hold")
    }

    fun resumeBill(billId: String) {
        val resumed = repository.resumeBill(billId)
        if (resumed != null) {
            _currentBill.value = resumed
            navigateTo(UiScreen.CounterBilling(resumed.billId))
            showFeedback("Resumed Bill $billId")
        }
    }

    fun cancelCurrentBill(reason: String) {
        val bill = _currentBill.value ?: return
        repository.cancelBill(bill.billId, reason)
        _currentBill.value = null
        navigateTo(UiScreen.CounterHome)
        showFeedback("Bill ${bill.billId} cancelled")
    }

    fun completePayment(paymentMode: String, customerPhone: String) {
        val bill = _currentBill.value ?: return
        val cashierId = _currentUser.value?.userId ?: "cashier"
        val (ok, paidBill) = repository.completePayment(bill.billId, paymentMode, customerPhone, cashierId)
        if (ok && paidBill != null) {
            _currentBill.value = paidBill
            navigateTo(UiScreen.CounterReceipt(paidBill.billId))
            showFeedback("Payment completed: ₹%.2f".format(paidBill.grandTotal))
        }
    }

    // --- Exit Guard Actions ---
    fun verifyGateExit(qrOrBillId: String) {
        val guardId = _currentUser.value?.userId ?: "guard1"
        val gateId = _currentUser.value?.assignedLocationId ?: "G01"
        val (status, bill, message) = repository.verifyGateExit(qrOrBillId, guardId, gateId)
        navigateTo(UiScreen.GuardResult(status, message, bill))
    }

    // --- Inventory Actions ---
    fun saveProduct(product: Product) {
        val userId = _currentUser.value?.userId ?: "staff"
        repository.addOrUpdateProduct(product, userId)
        showFeedback("Product ${product.name} saved")
    }

    fun adjustStock(sku: String, qty: Int, reason: String, note: String) {
        val userId = _currentUser.value?.userId ?: "staff"
        repository.adjustStock(sku, qty, reason, note, userId)
        showFeedback("Stock adjusted successfully")
    }

    // --- Settings & Connectivity ---
    fun testAppsScriptConnection(url: String, apiKey: String) {
        viewModelScope.launch {
            _connectionTestResult.value = "Testing connection..."
            val res = apiClient.testConnection(url, apiKey)
            if (res.isSuccess) {
                _connectionTestResult.value = "SUCCESS: Connected to \"${res.getOrNull()}\""
            } else {
                _connectionTestResult.value = "ERROR: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    private fun showFeedback(msg: String, isError: Boolean = false) {
        viewModelScope.launch {
            _feedbackMessage.value = (if (isError) "⚠️ " else "✓ ") + msg
            delay(2500)
            if (_feedbackMessage.value?.contains(msg) == true) {
                _feedbackMessage.value = null
            }
        }
    }
}
