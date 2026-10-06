package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BillingViewModel
import com.example.ui.viewmodel.UiScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmartBillingApp()
                }
            }
        }
    }
}

@Composable
fun SmartBillingApp(
    viewModel: BillingViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (val screen = currentScreen) {
        is UiScreen.DepartmentPortal -> {
            DepartmentPortalScreen(
                viewModel = viewModel,
                onSelectDepartment = { dept ->
                    viewModel.selectDepartment(dept)
                }
            )
        }

        is UiScreen.Login -> {
            BackHandler { viewModel.navigateTo(UiScreen.DepartmentPortal) }
            DepartmentLoginScreen(
                department = screen.department,
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.DepartmentPortal) }
            )
        }

        // --- Counter Flow ---
        is UiScreen.CounterHome -> {
            BackHandler { viewModel.navigateTo(UiScreen.DepartmentPortal) }
            CounterHomeScreen(
                viewModel = viewModel,
                onNewBill = { viewModel.startNewBill() },
                onResumeBill = { billId -> viewModel.resumeBill(billId) },
                onViewHistory = { viewModel.navigateTo(UiScreen.CounterHistory) },
                onDayClose = { viewModel.navigateTo(UiScreen.CounterDayClose) }
            )
        }

        is UiScreen.CounterBilling -> {
            BackHandler { viewModel.navigateTo(UiScreen.CounterHome) }
            CounterBillingScreen(
                billId = screen.billId,
                viewModel = viewModel,
                onProceedPayment = { billId -> viewModel.navigateTo(UiScreen.CounterPayment(billId)) },
                onBack = { viewModel.navigateTo(UiScreen.CounterHome) }
            )
        }

        is UiScreen.CounterPayment -> {
            BackHandler { viewModel.navigateTo(UiScreen.CounterBilling(screen.billId)) }
            CounterPaymentScreen(
                billId = screen.billId,
                viewModel = viewModel,
                onPaymentConfirmed = { billId -> viewModel.navigateTo(UiScreen.CounterReceipt(billId)) },
                onBack = { viewModel.navigateTo(UiScreen.CounterBilling(screen.billId)) }
            )
        }

        is UiScreen.CounterReceipt -> {
            BackHandler { viewModel.navigateTo(UiScreen.CounterHome) }
            CounterReceiptScreen(
                billId = screen.billId,
                viewModel = viewModel,
                onDoneNextCustomer = {
                    viewModel.navigateTo(UiScreen.CounterHome)
                }
            )
        }

        is UiScreen.CounterHistory -> {
            BackHandler { viewModel.navigateTo(UiScreen.CounterHome) }
            CounterHistoryScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.CounterHome) }
            )
        }

        is UiScreen.CounterDayClose -> {
            BackHandler { viewModel.navigateTo(UiScreen.CounterHome) }
            CounterDayCloseScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.CounterHome) }
            )
        }

        // --- Guard Flow ---
        is UiScreen.GuardScan -> {
            BackHandler { viewModel.navigateTo(UiScreen.DepartmentPortal) }
            GuardScanScreen(
                viewModel = viewModel,
                onViewHistory = { viewModel.navigateTo(UiScreen.GuardHistory) }
            )
        }

        is UiScreen.GuardResult -> {
            BackHandler { viewModel.navigateTo(UiScreen.GuardScan) }
            GuardResultScreen(
                status = screen.status,
                message = screen.message,
                bill = screen.bill,
                onScanNext = { viewModel.navigateTo(UiScreen.GuardScan) }
            )
        }

        is UiScreen.GuardHistory -> {
            BackHandler { viewModel.navigateTo(UiScreen.GuardScan) }
            GuardHistoryScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.GuardScan) }
            )
        }

        // --- Inventory Flow ---
        is UiScreen.InventoryHome -> {
            BackHandler { viewModel.navigateTo(UiScreen.DepartmentPortal) }
            InventoryHomeScreen(
                viewModel = viewModel,
                onOpenQrPrint = { viewModel.navigateTo(UiScreen.InventoryQrPrint) },
                onOpenAnalytics = { viewModel.navigateTo(UiScreen.InventoryAnalytics) }
            )
        }

        is UiScreen.InventoryQrPrint -> {
            BackHandler { viewModel.navigateTo(UiScreen.InventoryHome) }
            InventoryQrPrintScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.InventoryHome) }
            )
        }

        is UiScreen.InventoryAnalytics -> {
            BackHandler { viewModel.navigateTo(UiScreen.InventoryHome) }
            InventoryAnalyticsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.InventoryHome) }
            )
        }

        // --- Manager Flow ---
        is UiScreen.ManagerDashboard -> {
            BackHandler { viewModel.navigateTo(UiScreen.DepartmentPortal) }
            ManagerDashboardScreen(
                viewModel = viewModel,
                onOpenStaff = { viewModel.navigateTo(UiScreen.ManagerStaff) },
                onOpenSettings = { viewModel.navigateTo(UiScreen.ManagerSettings) },
                onOpenReports = { viewModel.navigateTo(UiScreen.ManagerReports) },
                onOpenAudit = { viewModel.navigateTo(UiScreen.ManagerAudit) }
            )
        }

        is UiScreen.ManagerStaff -> {
            BackHandler { viewModel.navigateTo(UiScreen.ManagerDashboard) }
            ManagerStaffScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.ManagerDashboard) }
            )
        }

        is UiScreen.ManagerSettings -> {
            BackHandler { viewModel.navigateTo(UiScreen.ManagerDashboard) }
            ManagerSettingsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.ManagerDashboard) }
            )
        }

        is UiScreen.ManagerReports -> {
            BackHandler { viewModel.navigateTo(UiScreen.ManagerDashboard) }
            ManagerReportsScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.ManagerDashboard) }
            )
        }

        is UiScreen.ManagerAudit -> {
            BackHandler { viewModel.navigateTo(UiScreen.ManagerDashboard) }
            ManagerAuditScreen(
                viewModel = viewModel,
                onBack = { viewModel.navigateTo(UiScreen.ManagerDashboard) }
            )
        }
    }
}
