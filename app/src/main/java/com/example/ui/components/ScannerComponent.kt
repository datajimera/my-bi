package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.DeepNavyPrimary
import com.example.ui.theme.TealAccent

@Composable
fun FastScannerBox(
    sampleProducts: List<Product> = emptyList(),
    onScan: (String) -> Unit,
    promptTitle: String = "Align QR Code inside frame",
    modifier: Modifier = Modifier
) {
    var isTorchOn by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableLongStateOf(0L) }
    var lastScannedCode by remember { mutableStateOf("") }

    // Laser sweep line animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    fun handleScanAttempt(code: String) {
        val now = System.currentTimeMillis()
        // Duplicate-frame guard (1.2s as specified in Section 4.2)
        if (code == lastScannedCode && now - lastScannedTime < 1200) {
            return
        }
        lastScannedCode = code
        lastScannedTime = now
        onScan(code)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A)) // Dark camera viewfinder
            .padding(12.dp)
    ) {
        // Top row: Scanner status & Torch toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676)) // Live Green status dot
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CONTINUOUS SCANNER ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            IconButton(
                onClick = { isTorchOn = !isTorchOn },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isTorchOn) TealAccent else Color.White.copy(alpha = 0.15f))
                    .testTag("torch_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Viewfinder target box with laser line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, if (isTorchOn) Color.Yellow else TealAccent, RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            // Scanner target reticle corners
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("┌", color = TealAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("┐", color = TealAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("└", color = TealAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("┘", color = TealAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Animated Laser line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (laserOffset * 130).dp)
                    .background(Color(0xFFFF1744)) // Neon Red laser
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = promptTitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Direct Quick-Scan Product Barcode chips for fast one-tap testing in emulator
        if (sampleProducts.isNotEmpty()) {
            Text(
                text = "Tap sample barcode to simulate physical camera scan:",
                color = Color.LightGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sampleProducts.take(6)) { p ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .clickable { handleScanAttempt(p.sku) }
                            .testTag("scan_chip_${p.sku}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${p.name.take(14)} (₹${p.sellPrice})",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Manual SKU / Barcode input row with instant submit
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualText,
                onValueChange = { manualText = it },
                placeholder = { Text("Or type SKU (e.g. SKU-1001) / Barcode", fontSize = 12.sp, color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (manualText.isNotBlank()) {
                        handleScanAttempt(manualText)
                        manualText = ""
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealAccent,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("manual_sku_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (manualText.isNotBlank()) {
                        handleScanAttempt(manualText)
                        manualText = ""
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                modifier = Modifier.height(52.dp).testTag("manual_scan_button")
            ) {
                Text("Scan", fontWeight = FontWeight.Bold)
            }
        }
    }
}
