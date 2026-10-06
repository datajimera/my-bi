package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.ui.theme.*
import com.example.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartBillingTopBar(
    title: String,
    department: Department?,
    onSwitchDepartment: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                if (department != null) {
                    Text(
                        text = department.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCCFBF1) // Teal accent light
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("top_bar_back_button")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.PointOfSale,
                    contentDescription = "App Logo",
                    tint = TealAccent,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp).size(28.dp)
                )
            }
        },
        actions = {
            AssistChip(
                onClick = onSwitchDepartment,
                label = { Text("Switch Dept", color = Color.White, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch Department",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                border = null,
                modifier = Modifier.padding(end = 8.dp).testTag("switch_dept_button")
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DeepNavyPrimary
        )
    )
}

@Composable
fun FeedbackBanner(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (message != null) {
            val isError = message.contains("⚠️") || message.contains("Invalid") || message.contains("not found")
            Surface(
                color = if (isError) ErrorRed else TealAccent,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    containerColor: Color = Color.White,
    iconColor: Color = DeepNavyPrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = DeepNavyPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TealAccent
                    )
                }
            }
        }
    }
}

@Composable
fun NumericKeypad(
    pinLength: Int = 4,
    enteredPin: String,
    onPinChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // PIN indicator dots
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            repeat(pinLength) { index ->
                val isFilled = index < enteredPin.length
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isFilled) DeepNavyPrimary else Color.LightGray)
                )
            }
        }

        // 3x4 Keypad buttons
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "OK")
        )

        for (row in keys) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (key in row) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                when (key) {
                                    "OK" -> TealAccent
                                    "C" -> Color(0xFFECEFF1)
                                    else -> Color.White
                                }
                            )
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .clickable {
                                when (key) {
                                    "C" -> {
                                        if (enteredPin.isNotEmpty()) {
                                            onPinChange(enteredPin.dropLast(1))
                                        }
                                    }
                                    "OK" -> onSubmit()
                                    else -> {
                                        if (enteredPin.length < pinLength) {
                                            val newPin = enteredPin + key
                                            onPinChange(newPin)
                                            if (newPin.length == pinLength) {
                                                // Trigger auto submit on reaching length
                                                onSubmit()
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("keypad_btn_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "C") {
                            Icon(Icons.Default.Backspace, contentDescription = "Clear", tint = Color.DarkGray)
                        } else {
                            Text(
                                text = key,
                                fontSize = if (key == "OK") 18.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (key == "OK") Color.White else DeepNavyPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QrCodeImage(
    qrText: String,
    sizeDp: Int = 180,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(qrText) {
        QrCodeGenerator.generateImageBitmap(qrText, 320)
    }

    androidx.compose.foundation.Image(
        bitmap = imageBitmap,
        contentDescription = "QR Code: $qrText",
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp)
    )
}
