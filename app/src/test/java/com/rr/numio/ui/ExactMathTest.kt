package com.rr.numio.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExactMathTest {

    private fun calc(expression: String): String? =
        ExactMath.evaluate(expression)?.let { formatExact(it) }

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
}
