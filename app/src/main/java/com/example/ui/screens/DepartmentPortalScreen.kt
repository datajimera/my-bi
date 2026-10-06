package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.ui.components.FeedbackBanner
import com.example.ui.components.NumericKeypad
import com.example.ui.theme.DeepNavyPrimary
import com.example.ui.theme.TealAccent
import com.example.ui.viewmodel.BillingViewModel

@Composable
fun DepartmentPortalScreen(
    viewModel: BillingViewModel,
    onSelectDepartment: (Department) -> Unit
) {
    val settings by viewModel.repository.settings.collectAsState()
    val syncStatus by viewModel.repository.syncStatus.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    Scaffold(
        topBar = {
            Surface(
                color = DeepNavyPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(TealAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = settings.storeName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Smart Billing Platform (4 Departments)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF99F6E4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (settings.appsScriptUrl.isNotBlank()) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (settings.appsScriptUrl.isNotBlank()) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (settings.appsScriptUrl.isNotBlank()) "Backend: Google Sheets Connected" else "Backend: Standalone Local Mode (Configure in Manager)",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            FeedbackBanner(feedbackMessage)

            Text(
                text = "Select Department to Login:",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepNavyPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(Department.values()) { dept ->
                    DepartmentCard(
                        department = dept,
                        onClick = { onSelectDepartment(dept) }
                    )
                }
            }

            // Quick Info Footer
            Surface(
                color = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Side-by-side Multi-Counter Architecture",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Default Demo PINs: Counter (1234) • Guard (4321) • Inventory (5678) • Manager (9999)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = DeepNavyPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DepartmentCard(
    department: Department,
    onClick: () -> Unit
) {
    val (icon, badgeColor) = when (department) {
        Department.COUNTER -> Pair(Icons.Default.PointOfSale, Color(0xFF1E88E5))
        Department.GUARD -> Pair(Icons.Default.Security, Color(0xFF43A047))
        Department.INVENTORY -> Pair(Icons.Default.Inventory2, Color(0xFFFB8C00))
        Department.MANAGER -> Pair(Icons.Default.AdminPanelSettings, DeepNavyPrimary)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("dept_card_${department.roleId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = department.displayName,
                    tint = badgeColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = department.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepNavyPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = department.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentLoginScreen(
    department: Department,
    viewModel: BillingViewModel,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${department.displayName} Login", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepNavyPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(DeepNavyPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (department) {
                        Department.COUNTER -> Icons.Default.PointOfSale
                        Department.GUARD -> Icons.Default.Security
                        Department.INVENTORY -> Icons.Default.Inventory2
                        Department.MANAGER -> Icons.Default.AdminPanelSettings
                    },
                    contentDescription = null,
                    tint = DeepNavyPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Enter 4-Digit Security PIN",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepNavyPrimary
            )

            Text(
                text = "Demo default PIN is: ${department.defaultPin}",
                style = MaterialTheme.typography.bodySmall,
                color = TealAccent
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFC62828),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            NumericKeypad(
                pinLength = 4,
                enteredPin = pin,
                onPinChange = {
                    pin = it
                    errorMessage = null
                },
                onSubmit = {
                    if (pin.length == 4) {
                        viewModel.login(pin) { success, msg ->
                            if (!success) {
                                errorMessage = msg
                                pin = ""
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
