package com.example.transportapp.core.common

/** Weight is always a whole number of grams in a [Long]. "1.5 tonne" is 1_500_000. */
@JvmInline
value class Weight(val grams: Long) {

    val kg: Long get() = grams / 1000

    operator fun plus(other: Weight): Weight = Weight(grams + other.grams)
    operator fun minus(other: Weight): Weight = Weight(grams - other.grams)
    operator fun times(count: Long): Weight = Weight(grams * count)

    fun toKg(): Double = grams / 1000.0

    /** "780 kg" with Indian grouping for large figures. */
    fun formattedKg(): String = formatIndianGrouping(kg) + " kg"

    /** Bare integer kg, used inside mixed captions. */
    fun kgValue(): String = formatIndianGrouping(kg)

    companion object {
        val ZERO = Weight(0)
        fun fromKg(kg: Long): Weight = Weight(kg * 1000)
    }
}
