@file:Suppress("NOTHING_TO_INLINE")

package com.github.panpf.zoomimage.core

import com.github.panpf.zoomimage.core.internal.lerp
import kotlin.math.roundToInt

data class IntOffsetCompat(val x: Int, val y: Int) {


    /**
     * Subtract a [IntOffsetCompat] from another one.
     */
    inline operator fun minus(other: IntOffsetCompat) =
        IntOffsetCompat(x - other.x, y - other.y)

    /**
     * Add a [IntOffsetCompat] to another one.
     */
    inline operator fun plus(other: IntOffsetCompat) =
        IntOffsetCompat(x + other.x, y + other.y)

    /**
     * Returns a new [IntOffsetCompat] representing the negation of this point.
     */
    inline operator fun unaryMinus() = IntOffsetCompat(-x, -y)

    /**
     * Multiplication operator.
     *
     * Returns an IntOffsetCompat whose coordinates are the coordinates of the
     * left-hand-side operand (an IntOffsetCompat) multiplied by the scalar
     * right-hand-side operand (a Float). The result is rounded to the nearest integer.
     */
    operator fun times(operand: Float): IntOffsetCompat = IntOffsetCompat(
        (x * operand).roundToInt(),
        (y * operand).roundToInt()
    )

    /**
     * Division operator.
     *
     * Returns an IntOffsetCompat whose coordinates are the coordinates of the
     * left-hand-side operand (an IntOffsetCompat) divided by the scalar right-hand-side
     * operand (a Float). The result is rounded to the nearest integer.
     */
    operator fun div(operand: Float): IntOffsetCompat = IntOffsetCompat(
        (x / operand).roundToInt(),
        (y / operand).roundToInt()
    )

    /**
     * Modulo (remainder) operator.
     *
     * Returns an IntOffsetCompat whose coordinates are the remainder of dividing the
     * coordinates of the left-hand-side operand (an IntOffsetCompat) by the scalar
     * right-hand-side operand (an Int).
     */
    operator fun rem(operand: Int) = IntOffsetCompat(x % operand, y % operand)

    override fun toString() = "OffsetCompat(${x}x${y})"

    companion object {
        val Zero = IntOffsetCompat(x = 0, y = 0)
    }
}

/**
 * Linearly interpolate between two [IntOffsetCompat] parameters
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid (and can
 * easily be generated by curves).
 *
 * Values for [fraction] are usually obtained from an [Animation<Float>], such as
 * an `AnimationController`.
 */
fun lerp(start: IntOffsetCompat, stop: IntOffsetCompat, fraction: Float): IntOffsetCompat {
    return IntOffsetCompat(
        lerp(start.x, stop.x, fraction),
        lerp(start.y, stop.y, fraction)
    )
}

operator fun OffsetCompat.plus(offset: IntOffsetCompat): OffsetCompat =
    OffsetCompat(x + offset.x, y + offset.y)

operator fun OffsetCompat.minus(offset: IntOffsetCompat): OffsetCompat =
    OffsetCompat(x - offset.x, y - offset.y)

operator fun IntOffsetCompat.plus(offset: OffsetCompat): OffsetCompat =
    OffsetCompat(x + offset.x, y + offset.y)

operator fun IntOffsetCompat.minus(offset: OffsetCompat): OffsetCompat =
    OffsetCompat(x - offset.x, y - offset.y)

/**
 * Converts the [IntOffsetCompat] to an [OffsetCompat].
 */
inline fun IntOffsetCompat.toCompatOffset() = OffsetCompat(x.toFloat(), y.toFloat())

/**
 * Round a [OffsetCompat] down to the nearest [Int] coordinates.
 */
inline fun OffsetCompat.roundToCompatIntOffset(): IntOffsetCompat =
    IntOffsetCompat(x.roundToInt(), y.roundToInt())


fun IntOffsetCompat.toShortString(): String = "${x}x${y}"

operator fun IntOffsetCompat.times(scaleFactor: ScaleFactorCompat): IntOffsetCompat {
    return IntOffsetCompat(
        x = (x * scaleFactor.scaleX).roundToInt(),
        y = (y * scaleFactor.scaleY).roundToInt(),
    )
}