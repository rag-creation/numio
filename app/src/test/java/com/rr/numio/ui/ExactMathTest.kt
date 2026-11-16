package com.rr.numio.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExactMathTest {

    private fun calc(expression: String, degrees: Boolean = true): String? =
        ExactMath.evaluate(expression, degrees)?.let { formatExact(it) }

    @Test
    fun bigNumbersKeepEveryDigit() {
        // Issue #1: Double gave 0 here
        assertEquals("1", calc("9007199254740993-9007199254740992"))
        assertEquals("9007199254740994", calc("9007199254740993+1"))
        assertEquals("9999999800000001", calc("99999999×99999999"))
    }

    @Test
    fun everydayMathStaysTheSame() {
        assertEquals("5", calc("2+3"))
        assertEquals("0.3", calc("0.1+0.2"))
        assertEquals("0.333333333333333", calc("1÷3"))
        assertEquals("0.666666666666667", calc("2÷3"))
        assertEquals("14", calc("2+3×4"))
        assertEquals("-6", calc("-2×3"))
        assertEquals("-6", calc("2×-3"))
        assertEquals("2.5", calc("5÷2"))
    }

    @Test
    fun percentWorksLikeBefore() {
        assertEquals("0.5", calc("50%"))
        assertEquals("50.1", calc("50+10%"))
        assertEquals("0.06", calc("2×3%"))
    }

    @Test
    fun brackets() {
        // Issue #4
        assertEquals("20", calc("(2+3)×4"))
        assertEquals("14", calc("2×(3+4)"))
        assertEquals("-5", calc("-(2+3)"))
        assertEquals("1", calc("(50+50)%"))
        assertEquals("21", calc("((1+2)×(3+4))"))
        assertEquals("2", calc("10÷(2+3)"))
        assertNull(ExactMath.evaluate("(2+3")) // the screen adds the missing ")" first
        assertNull(ExactMath.evaluate("()"))
    }

    @Test
    fun hugeAndTinyUseScientific() {
        assertEquals("1E25", calc("10000000000000×1000000000000"))
        assertEquals("1E-7", calc("1÷10000000"))
    }

    @Test
    fun otherInputFallsBackToExp4j() {
        assertNull(ExactMath.evaluate("1.5E20+1")) // result reused from history
        assertNull(ExactMath.evaluate("5÷0"))
        assertNull(ExactMath.evaluate("2+"))
        assertNull(ExactMath.evaluate(""))
    }

    @Test
    fun trigInDegrees() {
        assertEquals("0.5", calc("sin(30)"))
        assertEquals("1", calc("sin(90)"))
        assertEquals("0", calc("sin(180)"))
        assertEquals("-1", calc("sin(270)"))
        assertEquals("0.5", calc("cos(60)"))
        assertEquals("0", calc("cos(90)"))
        assertEquals("1", calc("tan(45)"))
        assertEquals("0", calc("tan(180)"))
        assertEquals("0.5", calc("sin(390)"))
        assertEquals("-0.5", calc("sin(-30)"))
        assertNull(calc("tan(90)"))
        assertNull(calc("tan(270)"))
    }

    @Test
    fun trigInRadians() {
        assertEquals("1", calc("sin(π÷2)", degrees = false))
        assertEquals("0", calc("sin(π)", degrees = false))
        assertEquals("-1", calc("cos(π)", degrees = false))
        assertEquals("0.841470984807897", calc("sin(1)", degrees = false))
    }

    @Test
    fun inverseTrig() {
        assertEquals("30", calc("sin⁻¹(0.5)"))
        assertEquals("90", calc("cos⁻¹(0)"))
        assertEquals("45", calc("tan⁻¹(1)"))
        assertEquals("1.5707963267949", calc("sin⁻¹(1)", degrees = false))
        assertNull(calc("sin⁻¹(2)"))
        assertNull(calc("cos⁻¹(-2)"))
    }

    @Test
    fun logsAndRoots() {
        assertEquals("0", calc("ln(1)"))
        assertEquals("1", calc("ln(e)"))
        assertEquals("3", calc("log(1000)"))
        assertEquals("3", calc("√(9)"))
        assertEquals("0.5", calc("√(0.25)"))
        assertEquals("1.4142135623731", calc("√(2)"))
        assertEquals("5", calc("abs(-5)"))
        assertNull(calc("ln(0)"))
        assertNull(calc("ln(-1)"))
        assertNull(calc("log(0)"))
        assertNull(calc("√(-4)"))
    }

    @Test
    fun powersFactorialsAndConstants() {
        assertEquals("1024", calc("2^10"))
        assertEquals("0.5", calc("2^-1"))
        assertEquals("0.25", calc("2^-2"))
        assertEquals("512", calc("2^3^2"))
        assertEquals("-4", calc("-2^2"))
        assertEquals("1.4142135623731", calc("2^0.5"))
        assertEquals("20", calc("(2+3)^2×0.8"))
        assertNull(calc("0^-1"))
        assertNull(calc("(-8)^(1÷3)"))
        assertEquals("25", calc("5²"))
        assertEquals("2.25", calc("1.5²"))
        assertEquals("10", calc("3²+1"))
        assertEquals("120", calc("5!"))
        assertEquals("1", calc("0!"))
        assertEquals("2432902008176640000", calc("20!"))
        assertNull(calc("3.5!"))
        assertNull(calc("(0-1)!"))
        assertNull(calc("1001!"))
        assertEquals("3.14159265358979", calc("π"))
        assertEquals("2.71828182845905", calc("e"))
        assertEquals("6.28318530717959", calc("2×π"))
        assertEquals("2.71828182845905", calc("e^(1)"))
    }

    @Test
    fun scientificMixedWithEverydayMaths() {
        assertEquals("4", calc("2×sin(30)+√(9)"))
        assertEquals("0.25", calc("1÷(4)"))
        assertEquals("100", calc("10^(2)"))
        assertEquals("1", calc("sin(30)+cos(60)"))
        assertEquals("7", calc("(3+4)×sin(90)"))
    }

    @Test
    fun brokenScientificInputIsRefused() {
        assertNull(calc("sin(30")) // the screen adds the missing ")" first
        assertNull(calc("sin30"))
        assertNull(calc("sin()"))
        assertNull(calc("foo(3)"))
        assertNull(calc("2π")) // the keypad types 2×π
        assertNull(calc("5!!!x"))
    }
}
