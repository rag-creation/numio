package com.rr.numio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val presetColors = listOf(
    "FFD23F" to "Yellow",
    "FF6B35" to "Orange",
    "E8452F" to "Red",
    "E91E8C" to "Pink",
    "9C27B0" to "Purple",
    "3F51B5" to "Indigo",
    "2196F3" to "Blue",
    "00BCD4" to "Cyan",
    "4CAF50" to "Green",
    "8BC34A" to "Lime",
    "FF5722" to "Deep Orange",
    "607D8B" to "Steel"
)

fun hexToColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (e: Exception) {
        Color(0xFFFFD23F)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    accentColor: String,
    onAccentColorChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val NumioBg = Color(0xFF1B1917)
    val NumioSurface = Color(0xFF3A3733)
    val NumioTextOnDark = Color(0xFFF2EFE9)
    val NumioTextMuted = Color(0xFF9C9578)
    val accent = hexToColor(accentColor)

    var customHex by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NumioBg)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NumioTextOnDark
                )
            }
            Text(
                text = "Settings",
                color = NumioTextOnDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Theme section
            Text(
                text = "THEME",
                color = NumioTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Preset color grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NumioSurface)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    presetColors.chunked(4).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowColors.forEach { (hex, name) ->
                                val isSelected = hex.uppercase() == accentColor.uppercase()
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(hexToColor(hex))
                                        .then(
                                            if (isSelected) Modifier.border(
                                                3.dp, NumioTextOnDark, CircleShape
                                            ) else Modifier
                                        )
                                        .clickable { onAccentColorChange(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Text("✓", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Custom hex input
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Custom hex color",
                        color = NumioTextMuted,
                        fontSize = 12.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("#", color = NumioTextOnDark, fontSize = 16.sp)
                        OutlinedTextField(
                            value = customHex,
                            onValueChange = {
                                customHex = it.take(6).uppercase()
                                customError = false
                            },
                            placeholder = { Text("FFD23F", color = NumioTextMuted) },
                            singleLine = true,
                            isError = customError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = NumioTextOnDark,
                                unfocusedTextColor = NumioTextOnDark,
                                focusedBorderColor = accent,
                                unfocusedBorderColor = NumioTextMuted,
                                cursorColor = accent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (customHex.length == 6) {
                                    onAccentColorChange(customHex)
                                    customError = false
                                } else {
                                    customError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (customError) {
                        Text(
                            text = "Enter a valid 6-character hex code",
                            color = Color(0xFFE8452F),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // About section
            Text(
                text = "ABOUT",
                color = NumioTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(NumioSurface)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Version
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", color = NumioTextOnDark, fontSize = 15.sp)
                        Text("1.0.0", color = NumioTextMuted, fontSize = 15.sp)
                    }

                    HorizontalDivider(color = NumioTextMuted.copy(alpha = 0.2f))

                    // Source code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO: open GitHub URL */ },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Source code", color = NumioTextOnDark, fontSize = 15.sp)
                        Text(
                            "GitHub ↗",
                            color = accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = NumioTextMuted.copy(alpha = 0.2f))

                    // Report a bug
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO: open GitHub issues URL */ },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Report a bug", color = NumioTextOnDark, fontSize = 15.sp)
                        Text(
                            "GitHub ↗",
                            color = accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = NumioTextMuted.copy(alpha = 0.2f))

                    // Privacy
                    Text(
                        text = "Numio stores all your calculation history only on your device. Nothing is ever sent anywhere.",
                        color = NumioTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    // Signature
                    Text(
                        text = "With Lo❤️e, RR",
                        color = accent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}