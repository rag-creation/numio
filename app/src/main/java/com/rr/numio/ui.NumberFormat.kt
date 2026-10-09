package com.rr.numio.ui

import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale
import kotlin.math.abs

/**
 * Turns a result into text for the display.
 *
 * Whole numbers are shown in full while a Double can hold them exactly (below 10^15).
 * Bigger or tiny numbers use scientific notation like 1.5E20, so nothing gets capped
 * (the old toLong() stopped at 9223372036854775807) and no exponent digit is lost.
 */
fun formatNumber(value: Double, significantDigits: Int = 12): String {
    if (value == 0.0) return "0"
    if (!value.isFinite()) return "Number too large"
    val size = abs(value)
    if (value % 1 == 0.0 && size < 1e15) return value.toLong().toString()
    if (size >= 1e15 || size < 1e-6) return scientific(value, significantDigits)
    // Round to 15 significant digits: hides binary leftovers like 0.1 + 0.2 = 0.30000000000000004
    return BigDecimal(value.toString()).round(MathContext(15)).stripTrailingZeros().toPlainString()
}

/** 1.5E20, 2E-7. The trailing zeros are trimmed from the number part only, never the exponent. */
internal fun scientific(value: Double, significantDigits: Int): String {
    if (!value.isFinite()) return "Number too large"
    val text = String.format(Locale.ROOT, "%.${significantDigits - 1}e", value) // 1.50000000000e+20
    val mantissa = text.substringBefore('e').trimEnd('0').trimEnd('.')
    val exponent = text.substringAfter('e').toInt()
    return "${mantissa}E$exponent"
}
