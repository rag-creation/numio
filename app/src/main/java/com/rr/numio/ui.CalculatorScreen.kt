package com.rr.numio.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rr.numio.data.HistoryEntity
import kotlinx.coroutines.delay

enum class KeyType { NUM, OP, BACKSPACE, CLEAR_ALL, EQUAL }

private val NumioBg = Color(0xFF1B1917)
private val NumioSurface = Color(0xFF3A3733)
private val NumioRed = Color(0xFFE8452F)
private val NumioTextMuted = Color(0xFF9C9578)
private val NumioTextOnDark = Color(0xFFF2EFE9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    accentColor: String = "FFD23F",
    onNavigateToSettings: () -> Unit = {},
    onNavigateToConverter: () -> Unit = {},
    viewModel: CalculatorViewModel = viewModel(
        factory = CalculatorViewModel.factory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.history.collectAsState()
    val accent = hexToColor(accentColor)
    val accentLight = hexToColor(accentColor).copy(alpha = 0.7f)
    val NumioYellowLight = accent.copy(alpha = 0.6f)

    var easterEggTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val keys = listOf(
        listOf("AC" to KeyType.CLEAR_ALL, "C" to KeyType.BACKSPACE, "%" to KeyType.OP, "÷" to KeyType.OP),
        listOf("7" to KeyType.NUM, "8" to KeyType.NUM, "9" to KeyType.NUM, "×" to KeyType.OP),
        listOf("4" to KeyType.NUM, "5" to KeyType.NUM, "6" to KeyType.NUM, "-" to KeyType.OP),
        listOf("1" to KeyType.NUM, "2" to KeyType.NUM, "3" to KeyType.NUM, "+" to KeyType.OP)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NumioBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Calc / Convert pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(NumioSurface)
                    .padding(4.dp)
            ) {
                Text(
                    text = "Calc",
                    color = NumioBg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
                Text(
                    text = "Convert",
                    color = NumioTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigateToConverter() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // App name — easter egg (7 taps within 3s)
            Text(
                text = "Numio",
                color = NumioTextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime > 3000L) easterEggTapCount = 0
                        lastTapTime = now
                        easterEggTapCount++
                        if (easterEggTapCount >= 7) {
                            easterEggTapCount = 0
                            viewModel.triggerEasterEgg()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )

            // History + Settings icons
            Row {
                IconButton(onClick = { viewModel.toggleHistory() }) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "View history",
                        tint = NumioTextMuted
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = NumioTextMuted
                    )
                }
            }
        }

        // Display card
        Box(
            modifier = Modifier
                .weight(0.28f)
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0E0D0C))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                val exprScrollState = rememberScrollState()
                LaunchedEffect(uiState.expression) {
                    exprScrollState.animateScrollTo(exprScrollState.maxValue)
                }
                Text(
                    text = uiState.expression.ifEmpty { "0" },
                    fontSize = 36.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    color = Color(0xFFCCBF8A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(exprScrollState)
                )
                if (uiState.result.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.result,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = NumioTextOnDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(accentLight)
                )
            }
        }

        // Keypad
        Column(
            modifier = Modifier
                .weight(0.72f)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { (label, type) ->
                        CircleKey(
                            label = label,
                            type = type,
                            accent = accent,
                            accentLight = NumioYellowLight,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { viewModel.onKeyPress(label) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircleKey(
                    label = "0",
                    type = KeyType.NUM,
                    accent = accent,
                    accentLight = NumioYellowLight,
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(999.dp),
                    onClick = { viewModel.onKeyPress("0") }
                )
                CircleKey(
                    label = ".",
                    type = KeyType.NUM,
                    accent = accent,
                    accentLight = NumioYellowLight,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { viewModel.onKeyPress(".") }
                )
                CircleKey(
                    label = "=",
                    type = KeyType.EQUAL,
                    accent = accent,
                    accentLight = NumioYellowLight,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { viewModel.onKeyPress("=") }
                )
            }
        }
    }

    // History bottom sheet
    if (uiState.isHistoryVisible) {
        ModalBottomSheet(onDismissRequest = { viewModel.toggleHistory() }) {
            HistorySheetContent(
                history = history,
                onItemClick = { viewModel.reuseHistoryEntry(it) },
                onClearAll = { viewModel.clearHistory() }
            )
        }
    }

    // Easter egg overlay
    if (uiState.isEasterEggVisible) {
        EasterEggScreen(
            accent = accent,
            onDismiss = { viewModel.dismissEasterEgg() }
        )
    }
}

@Composable
fun EasterEggScreen(accent: Color, onDismiss: () -> Unit) {
    var visibleLetters by remember { mutableIntStateOf(0) }
    var showThankYou by remember { mutableStateOf(false) }
    var showSignature by remember { mutableStateOf(false) }
    val fullName = "Numio"

    LaunchedEffect(Unit) {
        fullName.forEachIndexed { index, _ ->
            delay(150)
            visibleLetters = index + 1
        }
        delay(400)
        showThankYou = true
        delay(600)
        showSignature = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NumioBg)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Letter by letter name
            Text(
                text = fullName.take(visibleLetters),
                color = accent,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 6.sp
            )

            // Thank you message
            AnimatedVisibility(
                visible = showThankYou,
                enter = fadeIn(animationSpec = tween(600))
            ) {
                Text(
                    text = "Thanks for choosing Numio 💛",
                    color = NumioTextOnDark,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Signature
            AnimatedVisibility(
                visible = showSignature,
                enter = fadeIn(animationSpec = tween(800))
            ) {
                Text(
                    text = "With Lo❤️e, RR",
                    color = accent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Cursive
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "tap anywhere to close",
                color = NumioTextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CircleKey(
    label: String,
    type: KeyType,
    accent: Color,
    accentLight: Color,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    onClick: () -> Unit
) {
    val containerColor = when (type) {
        KeyType.NUM -> NumioSurface
        KeyType.OP -> accent
        KeyType.BACKSPACE -> NumioSurface
        KeyType.CLEAR_ALL -> NumioRed
        KeyType.EQUAL -> accentLight
    }
    val contentColor = when (type) {
        KeyType.NUM -> NumioTextOnDark
        KeyType.OP -> NumioBg
        KeyType.BACKSPACE -> NumioTextOnDark
        KeyType.CLEAR_ALL -> Color.White
        KeyType.EQUAL -> NumioBg
    }
    val fontSize = if (label == "AC" || label == "C") 15.sp else 26.sp
    val fontWeight = if (label == "AC" || label == "C") FontWeight.Bold else FontWeight.Medium

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = shape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = if (label == "C") "⌫" else label,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun HistorySheetContent(
    history: List<HistoryEntity>,
    onItemClick: (HistoryEntity) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "History", fontSize = 20.sp)
            if (history.isNotEmpty()) {
                TextButton(onClick = onClearAll) { Text("Clear all") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (history.isEmpty()) {
            Text(
                text = "No calculations yet",
                modifier = Modifier.padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(history, key = { it.id }) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(entry) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = entry.expression,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = "= ${entry.result}", fontSize = 20.sp)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}