package com.finora.android.core.model

/**
 * Represents a monetary amount in minor units (e.g., paise, cents) to ensure exact precision
 * and prevent floating-point inaccuracies per SRS constraint DC-2.
 */
@JvmInline
value class Amount(val minorUnits: Long) : Comparable<Amount> {
    companion object {
        val ZERO = Amount(0L)

        fun fromMajor(major: Long, minorUnitsPerMajor: Int = 100): Amount {
            return Amount(major * minorUnitsPerMajor)
        }

        fun fromDecimalString(value: String, minorUnitsPerMajor: Int = 100): Amount {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) return ZERO
            val parts = trimmed.split(".")
            val major = parts[0].toLongOrNull() ?: 0L
            val minor = if (parts.size > 1) {
                val fractional = parts[1].take(2).padEnd(2, '0')
                fractional.toLongOrNull() ?: 0L
            } else {
                0L
            }
            val sign = if (major < 0 || trimmed.startsWith("-")) -1L else 1L
            val absMajor = kotlin.math.abs(major)
            return Amount(sign * (absMajor * minorUnitsPerMajor + minor))
        }
    }

    operator fun plus(other: Amount): Amount = Amount(this.minorUnits + other.minorUnits)
    operator fun minus(other: Amount): Amount = Amount(this.minorUnits - other.minorUnits)
    operator fun unaryMinus(): Amount = Amount(-this.minorUnits)

    override fun compareTo(other: Amount): Int = this.minorUnits.compareTo(other.minorUnits)

    fun toFormattedString(currencySymbol: String = "₹"): String {
        val absUnits = kotlin.math.abs(minorUnits)
        val major = absUnits / 100
        val minor = absUnits % 100
        val prefix = if (minorUnits < 0) "-" else ""
        return if (minor == 0L) {
            "$prefix$currencySymbol$major"
        } else {
            String.format("%s%s%d.%02d", prefix, currencySymbol, major, minor)
        }
    }
}
