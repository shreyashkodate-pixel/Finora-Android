package com.finora.android.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountTest {

    @Test
    fun testAmountCreationAndAddition() {
        val amount1 = Amount.fromMajor(100)
        val amount2 = Amount.fromMajor(50)
        val total = amount1 + amount2

        assertEquals(15000L, total.minorUnits)
        assertEquals("₹150", total.toFormattedString())
    }

    @Test
    fun testAmountFromDecimalString() {
        val amount = Amount.fromDecimalString("149.50")
        assertEquals(14950L, amount.minorUnits)
        assertEquals("₹149.50", amount.toFormattedString())

        val amountWhole = Amount.fromDecimalString("200")
        assertEquals(20000L, amountWhole.minorUnits)
        assertEquals("₹200", amountWhole.toFormattedString())
    }

    @Test
    fun testAmountSubtraction() {
        val budget = Amount.fromMajor(1000)
        val spent = Amount.fromMajor(450)
        val remaining = budget - spent

        assertEquals(55000L, remaining.minorUnits)
        assertEquals("₹550", remaining.toFormattedString())
    }

    @Test
    fun testAmountComparison() {
        val a = Amount.fromMajor(50)
        val b = Amount.fromMajor(100)

        assertTrue(a < b)
        assertTrue(b > a)
    }
}
