package com.rr.numio.ui

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rr.numio.data.HistoryEntity
import kotlinx.coroutines.delay

enum class KeyType { NUM, OP, BACKSPACE, CLEAR_ALL, EQUAL, BRACKET }

private val NumioBg = Color(0xFF1B1917)
private val NumioSurface = Color(0xFF3A3733)
private val NumioRed = Color(0xFFE8452F)
private val NumioTextMuted = Color(0xFF9C9578)
private val NumioTextOnDark = Color(0xFFF2EFE9)
private val NumioSciSurface = Color(0xFF2A2825)

@SuppressLint("UnusedBoxWithConstraintsScope")
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
    // The scientific keys hide behind a handle; "2nd" swaps sin/cos/tan, ln/log for their inverses.
    var panelOpen by rememberSaveable { mutableStateOf(false) }
    var second by rememberSaveable { mutableStateOf(false) }

    val keys = listOf(
        listOf("AC" to KeyType.CLEAR_ALL, "C" to KeyType.BACKSPACE, "()" to KeyType.BRACKET, "%" to KeyType.OP, "÷" to KeyType.OP),
        listOf("7" to KeyType.NUM, "8" to KeyType.NUM, "9" to KeyType.NUM, "×" to KeyType.OP),
        listOf("4" to KeyType.NUM, "5" to KeyType.NUM, "6" to KeyType.NUM, "-" to KeyType.OP),
        listOf("1" to KeyType.NUM, "2" to KeyType.NUM, "3" to KeyType.NUM, "+" to KeyType.OP)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NumioBg)
            .statusBarsPadding()
            .navigationBarsPadding()
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

        // Landscape: display on the left, keypad on the right. Stacking them left the
        // display so short that big system fonts (150%) squeezed the result out of view.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (maxWidth > maxHeight) {
                val sciRowHeight = (maxHeight * 0.13f).coerceIn(28.dp, 40.dp)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // There's room in landscape, so the scientific keys are always shown
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        CalcDisplay(
                            expression = uiState.expression,
                            result = uiState.result,
                            cursor = uiState.cursor,
                            showCursor = uiState.showCursor,
                            onTapExpression = { viewModel.moveCursor(it) },
                            accentLight = accentLight,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        SciPanel(
                            second = second,
                            degrees = uiState.degrees,
                            accent = accent,
                            rowHeight = sciRowHeight,
                            onToggleSecond = { second = !second },
                            onToggleAngle = { viewModel.toggleAngleMode() },
                            onKey = { viewModel.onKeyPress(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        )
                    }
                    CalcKeypad(
                        keys = keys,
                        accent = accent,
                        accentLight = NumioYellowLight,
                        heightShare = 1f,
                        onKey = { viewModel.onKeyPress(it) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Display card — takes all space the keypad doesn't use
                    CalcDisplay(
                        expression = uiState.expression,
                        result = uiState.result,
                        cursor = uiState.cursor,
                        showCursor = uiState.showCursor,
                        onTapExpression = { viewModel.moveCursor(it) },
                        accentLight = accentLight,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                    SciHandle(
                        open = panelOpen,
                        accent = accent,
                        onSetOpen = { panelOpen = it }
                    )
                    AnimatedVisibility(
                        visible = panelOpen,
                        enter = expandVertically(tween(220)) + fadeIn(tween(220)),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
                    ) {
                        SciPanel(
                            second = second,
                            degrees = uiState.degrees,
                            accent = accent,
                            rowHeight = 44.dp,
                            onToggleSecond = { second = !second },
                            onToggleAngle = { viewModel.toggleAngleMode() },
                            onKey = { viewModel.onKeyPress(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }
                    CalcKeypad(
                        keys = keys,
                        accent = accent,
                        accentLight = NumioYellowLight,
                        // With the panel open the display gives up space so the keys keep their size
                        heightShare = if (panelOpen) 0.75f else 0.65f,
                        onKey = { viewModel.onKeyPress(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // History overlay (drawn in-screen, no separate window)
    if (uiState.isHistoryVisible) {
        HistoryOverlay(onDismiss = { viewModel.toggleHistory() }) {
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
    labelScale: Float = 1f,
    onClick: () -> Unit
) {
    val containerColor = when (type) {
        KeyType.NUM -> NumioSurface
        KeyType.OP -> accent
        KeyType.BACKSPACE -> NumioSurface
        KeyType.CLEAR_ALL -> NumioRed
        KeyType.EQUAL -> accentLight
        KeyType.BRACKET -> NumioSurface
    }
    val contentColor = when (type) {
        KeyType.NUM -> NumioTextOnDark
        KeyType.OP -> NumioBg
        KeyType.BACKSPACE -> NumioTextOnDark
        KeyType.CLEAR_ALL -> Color.White
        KeyType.EQUAL -> NumioBg
        KeyType.BRACKET -> accent
    }
    // Bigger labels: AC/⌫ 15 → 18, digits and operators 26 → 32
    val fontSize = (if (label == "AC" || label == "C") 18.sp else 32.sp) * labelScale
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
            text = when (label) {
                "C" -> "⌫"
                "()" -> "( )"
                else -> label
            },
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}

private class SciKeySpec(val label: String, val highlighted: Boolean, val onClick: () -> Unit)

/** The scientific keys: 5 across, 3 rows. "2nd" swaps in the inverse functions for one key press. */
@Composable
private fun SciPanel(
    second: Boolean,
    degrees: Boolean,
    accent: Color,
    rowHeight: Dp,
    onToggleSecond: () -> Unit,
    onToggleAngle: () -> Unit,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    fun plain(label: String) = SciKeySpec(label, false) { onKey(label) }
    fun swapped(label: String, inverse: String) = if (second) {
        SciKeySpec(inverse, false) {
            onKey(inverse)
            onToggleSecond()
        }
    } else {
        plain(label)
    }

    val rows = listOf(
        listOf(
            SciKeySpec("2nd", second, onToggleSecond),
            swapped("sin", "sin⁻¹"),
            swapped("cos", "cos⁻¹"),
            swapped("tan", "tan⁻¹"),
            SciKeySpec(if (degrees) "DEG" else "RAD", false, onToggleAngle)
        ),
        listOf(swapped("ln", "eˣ"), swapped("log", "10ˣ"), plain("√"), plain("x²"), plain("xʸ")),
        listOf(plain("π"), plain("e"), plain("!"), plain("1/x"), plain("|x|"))
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { spec ->
                    SciKey(
                        spec = spec,
                        accent = accent,
                        labelSize = (rowHeight.value * 0.42f).coerceIn(12f, 19f),
                        modifier = Modifier.weight(1f).height(rowHeight)
                    )
                }
            }
        }
    }
}

@Composable
private fun SciKey(spec: SciKeySpec, accent: Color, labelSize: Float, modifier: Modifier = Modifier) {
    val isMode = spec.label == "2nd" || spec.label == "DEG" || spec.label == "RAD"
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (spec.highlighted) accent else NumioSciSurface)
            .clickable(role = Role.Button, onClick = spec.onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = spec.label,
            color = if (spec.highlighted) NumioBg else if (isMode) accent else NumioTextOnDark,
            // Keep the label the same size at big system fonts, so "sin⁻¹" is never cut off
            fontSize = (labelSize / fontScale.coerceAtLeast(1f)).sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Tap, or swipe up or down, to show or hide the scientific keys. */
@Composable
private fun SciHandle(open: Boolean, accent: Color, onSetOpen: (Boolean) -> Unit) {
    val label = if (open) "Hide scientific keys" else "Show scientific keys"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .semantics {
                contentDescription = label
                role = Role.Button
                onClick(label = label) {
                    onSetOpen(!open)
                    true
                }
            }
            .pointerInput(open) { detectTapGestures { onSetOpen(!open) } }
            .pointerInput(open) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -24f) onSetOpen(true) else if (total > 24f) onSetOpen(false)
                    }
                ) { _, delta -> total += delta }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (open) accent else NumioTextMuted.copy(alpha = 0.6f))
        )
    }
}

@Composable
private fun HistoryOverlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss          // tap outside closes
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(                  // swallow taps inside the panel
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = NumioSurface,
            contentColor = NumioTextOnDark
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NumioTextMuted)
                )
                content()
            }
        }
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
                color = NumioTextMuted
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
                            color = NumioTextMuted
                        )
                        Text(text = "= ${entry.result}", fontSize = 20.sp)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

// Result text shrinks as the number gets longer, so it never gets cut off
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun CalcDisplay(
    expression: String,
    result: String,
    cursor: Int,
    showCursor: Boolean,
    onTapExpression: (Int) -> Unit,
    accentLight: Color,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .padding(vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF0E0D0C))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Shrink both lines to fit the card. Heights are in dp, font sizes in sp, so divide
        // by fontScale: at 150% system font 1sp is 1.5dp. Each line is set to 1.2× its font size.
        val fontScale = LocalDensity.current.fontScale
        val textDp = (maxHeight.value - 16f).coerceAtLeast(0f) // 4dp gap + 12dp underline
        val lineToSp = 1f / (1.2f * fontScale)
        val exprSize = minOf(48f, textDp * 0.40f * lineToSp).coerceAtLeast(8f)
        val resultSize = minOf(resultFontSize(result.length).value, textDp * 0.50f * lineToSp)
            .coerceAtLeast(8f)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.fillMaxWidth()
        ) {
            val exprScrollState = rememberScrollState()
            var exprLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
            val currentOnTap by rememberUpdatedState(onTapExpression)
            val exprColor = Color(0xFFCCBF8A)
            // Cursor in the middle, or shown after a tap (also at the end) until = or AC.
            val inMiddle = expression.isNotEmpty() && cursor < expression.length
            val editing = expression.isNotEmpty() && (showCursor || inMiddle)

            // Brackets still open get a faint ")" at the end: a hint that = will add them.
            val stillOpen = expression.count { it == '(' } - expression.count { it == ')' }
            val shown = buildAnnotatedString {
                append(expression.ifEmpty { "0" })
                if (stillOpen > 0) {
                    withStyle(SpanStyle(color = exprColor.copy(alpha = 0.35f))) {
                        append(")".repeat(stillOpen))
                    }
                }
            }

            val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
                initialValue = 1f,
                targetValue = 0.45f, // never fades out fully, so it's easy to spot
                animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
                label = "cursorBlink"
            )

            // Keep the end in view while typing at the end; keep the cursor in view while editing.
            LaunchedEffect(expression, cursor, exprLayout) {
                val layout = exprLayout ?: return@LaunchedEffect
                if (!inMiddle) {
                    exprScrollState.animateScrollTo(exprScrollState.maxValue)
                    return@LaunchedEffect
                }
                val x = layout.getCursorRect(cursor.coerceIn(0, layout.layoutInput.text.length))
                    .left.toInt()
                val viewport = exprScrollState.viewportSize
                val margin = 48
                if (viewport > 0) {
                    if (x < exprScrollState.value + margin) {
                        exprScrollState.animateScrollTo((x - margin).coerceAtLeast(0))
                    } else if (x > exprScrollState.value + viewport - margin) {
                        exprScrollState.animateScrollTo(x - viewport + margin)
                    }
                }
            }

            // Expression — bigger and right-aligned. weight(fill = false) measures it last,
            // so if space ever runs out the expression gets clipped, never the result.
            // Tap anywhere in it to put the cursor there and fix a number in the middle.
            Text(
                text = shown,
                fontSize = exprSize.sp,
                lineHeight = (exprSize * 1.2f).sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                color = exprColor,
                onTextLayout = { exprLayout = it },
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .horizontalScroll(exprScrollState)
                    .pointerInput(Unit) {
                        detectTapGestures { position ->
                            exprLayout?.let { currentOnTap(it.getOffsetForPosition(position)) }
                        }
                    }
                    .drawWithContent {
                        drawContent()
                        val layout = exprLayout
                        if (editing && layout != null) {
                            val r = layout.getCursorRect(
                                cursor.coerceIn(0, layout.layoutInput.text.length)
                            )
                            drawLine(
                                color = accentLight.copy(alpha = blink),
                                start = Offset(r.left, r.top),
                                end = Offset(r.left, r.bottom),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
            )
            if (result.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                // Result — right-aligned, shrinks for long numbers
                Text(
                    text = result,
                    fontSize = resultSize.sp,
                    lineHeight = (resultSize * 1.2f).sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
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
}

/** Round keys. [heightShare] is how much of the available height the keys may use. */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun CalcKeypad(
    keys: List<List<Pair<String, KeyType>>>,
    accent: Color,
    accentLight: Color,
    heightShare: Float,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.padding(bottom = 16.dp)
    ) {
        val gap = 12.dp
        // Size keys by width, but cap by height so landscape still fits
        // The top row has 5 smaller keys (AC ⌫ ( ) % ÷) across the same width as 4 keys:
        // 5 × small + 4 gaps = 4 × key + 3 gaps, so small = (4 × key − gap) / 5.
        // Height: small + 4 keys + 4 gaps must fit, which gives the byHeight formula.
        val byWidth = (maxWidth - gap * 3) / 4
        val byHeight = ((maxHeight * heightShare - gap * 4) * 5 + gap) / 24
        val keySize = minOf(byWidth, byHeight)
        val smallKeySize = (keySize * 4 - gap) / 5
        // Smaller keys (landscape) get smaller labels, so "AC" and "⌫" aren't clipped
        val labelScale = (keySize / 80.dp).coerceIn(0.45f, 1f)
        val smallLabelScale = (smallKeySize / 80.dp).coerceIn(0.45f, 1f)

        Column(
            verticalArrangement = Arrangement.spacedBy(gap),
            modifier = Modifier.align(Alignment.Center)
        ) {
            keys.forEach { row ->
                val small = row.size == 5
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { (label, type) ->
                        CircleKey(
                            label = label,
                            type = type,
                            accent = accent,
                            accentLight = accentLight,
                            labelScale = if (small) smallLabelScale else labelScale,
                            modifier = Modifier.size(if (small) smallKeySize else keySize),
                            onClick = { onKey(label) }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                CircleKey(
                    label = "0",
                    type = KeyType.NUM,
                    accent = accent,
                    accentLight = accentLight,
                    labelScale = labelScale,
                    modifier = Modifier
                        .width(keySize * 2 + gap)
                        .height(keySize),
                    shape = RoundedCornerShape(999.dp),
                    onClick = { onKey("0") }
                )
                CircleKey(
                    label = ".",
                    type = KeyType.NUM,
                    accent = accent,
                    accentLight = accentLight,
                    labelScale = labelScale,
                    modifier = Modifier.size(keySize),
                    onClick = { onKey(".") }
                )
                CircleKey(
                    label = "=",
                    type = KeyType.EQUAL,
                    accent = accent,
                    accentLight = accentLight,
                    labelScale = labelScale,
                    modifier = Modifier.size(keySize),
                    onClick = { onKey("=") }
                )
            }
        }
    }
}

private fun resultFontSize(length: Int) = when {
    length <= 8 -> 58.sp
    length <= 12 -> 46.sp
    length <= 16 -> 36.sp
    length <= 20 -> 28.sp
    else -> 24.sp
}