package com.rr.numio.ui

import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale

/**
 * Exact maths for what the keypad can type: numbers, + - × ÷ % ^ ² ! brackets, π, e and the
 * scientific functions (sin cos tan and their inverses, ln, log, √, abs).
 *
 * exp4j works in Double, which only keeps about 16 digits, so
 * 9007199254740993 − 9007199254740992 came out as 0 instead of 1.
 * BigDecimal keeps every digit for + − × and 34 digits for ÷.
 * Functions like sin or ln have no exact form, so they are worked out in Double and
 * rounded to 15 digits (sin 30° gives 0.5, not 0.49999999999999994).
 *
 * Returns null when the text has anything else in it (like 1.5E20 reused from history)
 * or can't be worked out (like ÷ 0 or ln(-1)), so the caller can show an error.
 */
internal object ExactMath {
    private val DIVIDE = MathContext.DECIMAL128
    private val ROUND = MathContext(15)
    private val HUNDRED = BigDecimal(100)
    private val FULL_TURN = BigDecimal(360)
    private val QUARTER_TURN = BigDecimal(90)
    private val PI = BigDecimal("3.14159265358979323846264338327950288")
    private val E = BigDecimal("2.71828182845904523536028747135266250")
    private const val BASIC = "0123456789.+-×÷%()^²!"
    private const val MAX_FACTORIAL = 1000
    private const val MAX_INTEGER_POWER = 9999

    /** Longest first, so "sin⁻¹" is not read as "sin". Each is followed by "(". */
    private val FUNCTIONS = listOf("sin⁻¹", "cos⁻¹", "tan⁻¹", "sin", "cos", "tan", "ln", "log", "abs", "√")

    private fun allowed(c: Char) = c in BASIC || c in 'a'..'z' || c == 'π' || c == '√' || c == '⁻' || c == '¹'

    fun evaluate(expression: String, degrees: Boolean = true): BigDecimal? {
        if (expression.isEmpty() || !expression.all { allowed(it) }) return null
        return try {
            Parser(expression, degrees).parse()
        } catch (e: NumberFormatException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }

    /**
     * expression := term (+|- term)* ; term := unary (×|÷ unary)*
     * unary := -unary | power ; power := postfix (^ unary)?
     * postfix := atom (% | ² | !)* ; atom := number | π | e | ( expression ) | name ( expression )
     */
    private class Parser(private val s: String, private val degrees: Boolean) {
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
            return power()
        }

        private fun power(): BigDecimal {
            val base = postfix()
            if (peek() == '^') {
                pos++
                return pow(base, unary())
            }
            return base
        }

        private fun postfix(): BigDecimal {
            var value = atom()
            while (true) {
                value = when (peek()) {
                    '%' -> value.divide(HUNDRED, DIVIDE)
                    '²' -> value.multiply(value)
                    '!' -> factorial(value)
                    else -> return value
                }
                pos++
            }
        }

        private fun atom(): BigDecimal {
            val c = peek() ?: throw NumberFormatException("Unexpected end")
            return when {
                c == '(' -> {
                    pos++
                    closeBracket(expression())
                }
                c == 'π' -> { pos++; PI }
                c == 'e' -> { pos++; E }
                c in '0'..'9' || c == '.' -> number()
                else -> {
                    val name = FUNCTIONS.firstOrNull { s.startsWith(it, pos) }
                        ?: throw NumberFormatException("Unknown '$c' at $pos")
                    pos += name.length
                    if (peek() != '(') throw NumberFormatException("Missing ( after $name")
                    pos++
                    callFunction(name, closeBracket(expression()))
                }
            }
        }

        private fun closeBracket(value: BigDecimal): BigDecimal {
            if (peek() != ')') throw NumberFormatException("Missing ) at $pos")
            pos++
            return value
        }

        private fun number(): BigDecimal {
            val start = pos
            while (pos < s.length && (s[pos] in '0'..'9' || s[pos] == '.')) pos++
            if (start == pos) throw NumberFormatException("Number expected at $start")
            return BigDecimal(s.substring(start, pos))
        }

        private fun callFunction(name: String, x: BigDecimal): BigDecimal = when (name) {
            "sin", "cos", "tan" -> trig(name, x)
            "sin⁻¹", "cos⁻¹", "tan⁻¹" -> inverseTrig(name, x)
            "ln" -> {
                if (x.signum() <= 0) throw ArithmeticException("ln of $x")
                fromDouble(Math.log(x.toDouble()))
            }
            "log" -> {
                if (x.signum() <= 0) throw ArithmeticException("log of $x")
                fromDouble(Math.log10(x.toDouble()))
            }
            "√" -> {
                if (x.signum() < 0) throw ArithmeticException("√ of $x")
                fromDouble(Math.sqrt(x.toDouble()))
            }
            "abs" -> x.abs()
            else -> throw NumberFormatException("Unknown function $name")
        }

        private fun trig(name: String, x: BigDecimal): BigDecimal {
            val radians: Double
            if (degrees) {
                var turn = x.remainder(FULL_TURN)
                if (turn.signum() < 0) turn = turn.add(FULL_TURN)
                // 0°, 90°, 180°, 270° are exact: sin 180° is 0, not 1.2E-16
                if (turn.remainder(QUARTER_TURN).signum() == 0) {
                    val quarter = turn.toInt() / 90
                    return BigDecimal(
                        when (name) {
                            "sin" -> intArrayOf(0, 1, 0, -1)[quarter]
                            "cos" -> intArrayOf(1, 0, -1, 0)[quarter]
                            else -> {
                                if (quarter % 2 == 1) throw ArithmeticException("tan of 90°")
                                0
                            }
                        }
                    )
                }
                radians = Math.toRadians(turn.toDouble())
            } else {
                radians = x.toDouble()
            }
            val value = when (name) {
                "sin" -> Math.sin(radians)
                "cos" -> Math.cos(radians)
                else -> Math.tan(radians)
            }
            if (Math.abs(value) < 1e-15 && Math.abs(radians) > 1e-3) return BigDecimal.ZERO
            if (Math.abs(value) > 1e15) throw ArithmeticException("$name is undefined here")
            return fromDouble(value)
        }

        private fun inverseTrig(name: String, x: BigDecimal): BigDecimal {
            val v = x.toDouble()
            if (name != "tan⁻¹" && (v < -1.0 || v > 1.0)) throw ArithmeticException("$name of $x")
            val radians = when (name) {
                "sin⁻¹" -> Math.asin(v)
                "cos⁻¹" -> Math.acos(v)
                else -> Math.atan(v)
            }
            return fromDouble(if (degrees) Math.toDegrees(radians) else radians)
        }

        private fun pow(base: BigDecimal, exponent: BigDecimal): BigDecimal {
            val whole = exponent.stripTrailingZeros().scale() <= 0
            if (whole && exponent.abs() <= BigDecimal(MAX_INTEGER_POWER)) {
                val n = exponent.toInt()
                val p = base.pow(Math.abs(n), DIVIDE)
                return if (n >= 0) p else BigDecimal.ONE.divide(p, DIVIDE)
            }
            return fromDouble(Math.pow(base.toDouble(), exponent.toDouble()))
        }

        private fun factorial(x: BigDecimal): BigDecimal {
            val n = x.toBigIntegerExact()
            if (n.signum() < 0 || n > java.math.BigInteger.valueOf(MAX_FACTORIAL.toLong())) {
                throw ArithmeticException("factorial of $x")
            }
            var result = java.math.BigInteger.ONE
            for (i in 2..n.toInt()) result = result.multiply(java.math.BigInteger.valueOf(i.toLong()))
            return BigDecimal(result)
        }

        /** A Double result as a 15-digit BigDecimal; null-like cases (NaN, ∞) are errors. */
        private fun fromDouble(d: Double): BigDecimal {
            if (d.isNaN() || d.isInfinite()) throw ArithmeticException("not a number")
            return BigDecimal.valueOf(d).round(ROUND)
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
