package com.rr.numio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ConversionUnit(val name: String, val toBase: Double)

data class ConversionCategory(
    val name: String,
    val units: List<ConversionUnit>
)

val conversionCategories = listOf(
    ConversionCategory("Length", listOf(
        ConversionUnit("Millimeter", 0.001),
        ConversionUnit("Centimeter", 0.01),
        ConversionUnit("Meter", 1.0),
        ConversionUnit("Kilometer", 1000.0),
        ConversionUnit("Inch", 0.0254),
        ConversionUnit("Foot", 0.3048),
        ConversionUnit("Yard", 0.9144),
        ConversionUnit("Mile", 1609.344),
        ConversionUnit("Nautical Mile", 1852.0)
    )),
    ConversionCategory("Weight", listOf(
        ConversionUnit("Milligram", 0.000001),
        ConversionUnit("Gram", 0.001),
        ConversionUnit("Kilogram", 1.0),
        ConversionUnit("Metric Ton", 1000.0),
        ConversionUnit("Ounce", 0.0283495),
        ConversionUnit("Pound", 0.453592),
        ConversionUnit("Stone", 6.35029)
    )),
    ConversionCategory("Area", listOf(
        ConversionUnit("Square Millimeter", 0.000001),
        ConversionUnit("Square Centimeter", 0.0001),
        ConversionUnit("Square Meter", 1.0),
        ConversionUnit("Square Kilometer", 1000000.0),
        ConversionUnit("Square Inch", 0.00064516),
        ConversionUnit("Square Foot", 0.092903),
        ConversionUnit("Square Yard", 0.836127),
        ConversionUnit("Acre", 4046.856),
        ConversionUnit("Hectare", 10000.0)
    )),
    ConversionCategory("Volume", listOf(
        ConversionUnit("Milliliter", 0.001),
        ConversionUnit("Liter", 1.0),
        ConversionUnit("Cubic Meter", 1000.0),
        ConversionUnit("Teaspoon (US)", 0.00492892),
        ConversionUnit("Tablespoon (US)", 0.0147868),
        ConversionUnit("Fluid Ounce (US)", 0.0295735),
        ConversionUnit("Cup (US)", 0.236588),
        ConversionUnit("Pint (US)", 0.473176),
        ConversionUnit("Quart (US)", 0.946353),
        ConversionUnit("Gallon (US)", 3.78541)
    )),
    ConversionCategory("Temperature", listOf(
        ConversionUnit("Celsius", 1.0),
        ConversionUnit("Fahrenheit", 1.0),
        ConversionUnit("Kelvin", 1.0)
    )),
    ConversionCategory("Time", listOf(
        ConversionUnit("Millisecond", 0.001),
        ConversionUnit("Second", 1.0),
        ConversionUnit("Minute", 60.0),
        ConversionUnit("Hour", 3600.0),
        ConversionUnit("Day", 86400.0),
        ConversionUnit("Week", 604800.0),
        ConversionUnit("Month (avg)", 2629800.0),
        ConversionUnit("Year", 31557600.0)
    )),
    ConversionCategory("Speed", listOf(
        ConversionUnit("m/s", 1.0),
        ConversionUnit("km/h", 0.277778),
        ConversionUnit("mph", 0.44704),
        ConversionUnit("Knot", 0.514444),
        ConversionUnit("ft/s", 0.3048),
        ConversionUnit("Mach", 343.0)
    )),
    ConversionCategory("Pressure", listOf(
        ConversionUnit("Pascal", 1.0),
        ConversionUnit("Kilopascal", 1000.0),
        ConversionUnit("Megapascal", 1000000.0),
        ConversionUnit("Bar", 100000.0),
        ConversionUnit("Millibar", 100.0),
        ConversionUnit("PSI", 6894.757),
        ConversionUnit("Atmosphere", 101325.0),
        ConversionUnit("mmHg", 133.322)
    ))
)

fun convertTemperature(value: Double, from: String, to: String): Double {
    val celsius = when (from) {
        "Fahrenheit" -> (value - 32) * 5 / 9
        "Kelvin" -> value - 273.15
        else -> value
    }
    return when (to) {
        "Fahrenheit" -> celsius * 9 / 5 + 32
        "Kelvin" -> celsius + 273.15
        else -> celsius
    }
}

fun convert(value: Double, from: ConversionUnit, to: ConversionUnit, category: String): Double {
    if (category == "Temperature") return convertTemperature(value, from.name, to.name)
    val base = value * from.toBase
    return base / to.toBase
}

fun formatResult(value: Double): String {
    if (value == 0.0) return "0"
    if (value % 1 == 0.0 && kotlin.math.abs(value) < 1e12) return value.toLong().toString()
    return "%.6g".format(value).trimEnd('0').trimEnd('.')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterScreen(
    accentColor: String,
    onBack: () -> Unit
) {
    val NumioBg = Color(0xFF1B1917)
    val NumioSurface = Color(0xFF3A3733)
    val NumioTextOnDark = Color(0xFFF2EFE9)
    val NumioTextMuted = Color(0xFF9C9578)
    val accent = hexToColor(accentColor)

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val category = conversionCategories[selectedCategoryIndex]

    var fromUnitIndex by remember { mutableIntStateOf(0) }
    var toUnitIndex by remember { mutableIntStateOf(1) }
    var inputValue by remember { mutableStateOf("") }

    // Reset unit indices when category changes
    LaunchedEffect(selectedCategoryIndex) {
        fromUnitIndex = 0
        toUnitIndex = 1
        inputValue = ""
    }

    val result = remember(inputValue, fromUnitIndex, toUnitIndex, selectedCategoryIndex) {
        val v = inputValue.toDoubleOrNull()
        if (v == null) ""
        else formatResult(convert(v, category.units[fromUnitIndex], category.units[toUnitIndex], category.name))
    }

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
                text = "Converter",
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
            // Category chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                conversionCategories.forEachIndexed { index, cat ->
                    FilterChip(
                        selected = selectedCategoryIndex == index,
                        onClick = { selectedCategoryIndex = index },
                        label = {
                            Text(
                                cat.name,
                                fontSize = 12.sp,
                                fontWeight = if (selectedCategoryIndex == index)
                                    FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Color.Black,
                            containerColor = NumioSurface,
                            labelColor = NumioTextMuted
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Conversion card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(NumioSurface)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // From unit
                    Text("From", color = NumioTextMuted, fontSize = 12.sp)
                    UnitDropdown(
                        units = category.units,
                        selectedIndex = fromUnitIndex,
                        onSelect = { fromUnitIndex = it },
                        accent = accent,
                        surface = NumioSurface,
                        textColor = NumioTextOnDark,
                        mutedColor = NumioTextMuted
                    )

                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        placeholder = { Text("Enter value", color = NumioTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NumioTextOnDark,
                            unfocusedTextColor = NumioTextOnDark,
                            focusedBorderColor = accent,
                            unfocusedBorderColor = NumioTextMuted,
                            cursorColor = accent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Swap button
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = {
                                val temp = fromUnitIndex
                                fromUnitIndex = toUnitIndex
                                toUnitIndex = temp
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(accent)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = "Swap",
                                tint = Color.Black
                            )
                        }
                    }

                    // To unit
                    Text("To", color = NumioTextMuted, fontSize = 12.sp)
                    UnitDropdown(
                        units = category.units,
                        selectedIndex = toUnitIndex,
                        onSelect = { toUnitIndex = it },
                        accent = accent,
                        surface = NumioSurface,
                        textColor = NumioTextOnDark,
                        mutedColor = NumioTextMuted
                    )

                    // Result
                    if (result.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(NumioBg)
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("Result", color = NumioTextMuted, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$result ${category.units[toUnitIndex].name}",
                                    color = accent,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun UnitDropdown(
    units: List<ConversionUnit>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    accent: Color,
    surface: Color,
    textColor: Color,
    mutedColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val NumioBg = Color(0xFF1B1917)

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NumioBg)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(units[selectedIndex].name, color = textColor, fontSize = 15.sp)
            Text("▾", color = accent, fontSize = 16.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(surface)
        ) {
            units.forEachIndexed { index, unit ->
                DropdownMenuItem(
                    text = {
                        Text(
                            unit.name,
                            color = if (index == selectedIndex) accent else textColor,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    }
                )
            }
        }
    }
}