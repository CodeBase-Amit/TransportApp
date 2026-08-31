package com.example.transportapp.domain.transport.calc

/**
 * §10.1 chargeable weight, pure over integer grams. Volumetric weight is per-package
 * L×B×H in centimetres divided by the company's divisor, times the package count; the
 * chargeable weight is the greater of actual and volumetric, rounded up to the company's
 * weight step. The rate row's minimum quantity (a pricing floor) is applied later, at
 * freight time — it is rate data, not a property of the goods.
 */
data class PackageDims(val lengthCm: Long, val breadthCm: Long, val heightCm: Long)

object ChargeableWeight {

    fun volumetricG(dims: PackageDims, packageCount: Long, divisor: Long): Long {
        require(divisor > 0) { "volumetric divisor must be positive" }
        require(packageCount >= 0)
        val volumeCm3 = dims.lengthCm * dims.breadthCm * dims.heightCm
        return ceilDiv(volumeCm3 * packageCount * 1000L, divisor)
    }

    fun chargeableG(actualG: Long, volumetricG: Long?, weightStepG: Long): Long {
        require(actualG >= 0)
        require(weightStepG > 0) { "weight step must be positive" }
        val base = maxOf(actualG, volumetricG ?: 0L)
        val remainder = base % weightStepG
        return if (remainder == 0L) base else base + (weightStepG - remainder)
    }

    /** Ceil division for non-negative values. */
    fun ceilDiv(numerator: Long, denominator: Long): Long =
        (numerator + denominator - 1) / denominator
}
