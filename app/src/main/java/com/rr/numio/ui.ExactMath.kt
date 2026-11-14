package com.rr.numio.ui

import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale

/**
 * Exact maths for what the keypad can type: numbers, + - × ÷ % and brackets.
 *
 * exp4j works in Double, which only keeps about 16 digits, so
 * 9007199254740993 − 9007199254740992 came out as 0 instead of 1.
 * BigDecimal keeps every digit for + − × and 34 digits for ÷.
 *
 * Returns null when the text has anything else in it (like 1.5E20 reused from history)
 * or can't be worked out (like ÷ 0), so the caller can fall back to exp4j.
 */
internal object ExactMath {
    private val DIVIDE = MathContext.DECIMAL128
    private val HUNDRED = BigDecimal(100)
    private const val ALLOWED = "0123456789.+-×÷%()"

    fun evaluate(expression: String): BigDecimal? {
        if (expression.isEmpty() || expression.any { it !in ALLOWED }) return null
        return try {
            Parser(expression).parse()
        } catch (e: NumberFormatException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }

    /**
     * expression := term (+|- term)* ; term := unary (×|÷ unary)*
     * unary := -unary | (number | "(" expression ")") %*
     */
    private class Parser(private val s: String) {
        private var pos = 0

        fun parse(): BigDecimal {
            val value = expression()
            if (pos != s.length) throw NumberFormatException("Unexpected '${s[pos]}' at $pos")
            return value
        }

        private fun peek(): Char? = if (pos < s.length) s[pos] else null

        private fun expression(): BigDecimal {
            var value = term()
            while (peek() == '+' || peek() == '-') {
                val op = s[pos++]
                val right = term()
                value = if (op == '+') value.add(right) else value.subtract(right)
            }
            return value
        }

        private fun term(): BigDecimal {
            var value = unary()
            while (peek() == '×' || peek() == '÷') {
                val op = s[pos++]
                val right = unary()
                value = if (op == '×') value.multiply(right) else value.divide(right, DIVIDE)
            }
            return value
        }

        private fun unary(): BigDecimal {
            if (peek() == '-') {
                pos++
                return unary().negate()
            }
            var value = if (peek() == '(') {
                pos++
                val inside = expression()
                if (peek() != ')') throw NumberFormatException("Missing ) at $pos")
                pos++
                inside
            } else {
                number()
            }
            while (peek() == '%') {
                pos++
                value = value.divide(HUNDRED, DIVIDE)
            }
            return value
        }

        private fun number(): BigDecimal {
            val start = pos
            while (pos < s.length && (s[pos] in '0'..'9' || s[pos] == '.')) pos++
            if (start == pos) throw NumberFormatException("Number expected at $start")
            return BigDecimal(s.substring(start, pos))
        }
    }
}

private val SCIENTIFIC_ABOVE = BigDecimal("1E15")
private val SCIENTIFIC_BELOW = BigDecimal("1E-6")

/**
 * Display text for an exact result. Whole numbers up to 20 digits are shown in full,
 * everything else follows the same rules as [formatNumber].
 */
fun formatExact(value: BigDecimal, significantDigits: Int = 12): String {
    if (value.signum() == 0) return "0"
    val plain = value.stripTrailingZeros()
    val digits = plain.precision() - plain.scale()
    if (plain.scale() <= 0 && digits <= 20) return plain.toPlainString()
    val size = plain.abs()
    if (size >= SCIENTIFIC_ABOVE || size < SCIENTIFIC_BELOW) {
        val text = String.format(Locale.ROOT, "%.${significantDigits - 1}e", plain)
        val mantissa = text.substringBefore('e').trimEnd('0').trimEnd('.')
        val exponent = text.substringAfter('e').toInt()
        return "${mantissa}E$exponent"
    }
    return plain.round(MathContext(15)).stripTrailingZeros().toPlainString()
}
