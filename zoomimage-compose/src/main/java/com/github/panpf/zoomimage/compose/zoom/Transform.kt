package com.github.panpf.zoomimage.compose.zoom

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ScaleFactor
import com.github.panpf.zoomimage.compose.internal.TopStart
import com.github.panpf.zoomimage.compose.internal.format
import com.github.panpf.zoomimage.compose.internal.times
import com.github.panpf.zoomimage.compose.internal.toShortString

data class Transform(
    val scale: ScaleFactor,
    val offset: Offset, // todo IntOffset
    val rotation: Float = 0f,   // todo Int
    val origin: TransformOrigin = TransformOrigin.TopStart,
) {

    constructor(
        scaleX: Float,
        scaleY: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float = 0f,
        originX: Float = 0f,
        originY: Float = 0f,
    ) : this(
        scale = ScaleFactor(scaleX = scaleX, scaleY = scaleY),
        offset = Offset(x = offsetX, y = offsetY),
        rotation = rotation,
        origin = TransformOrigin(pivotFractionX = originX, pivotFractionY = originY)
    )

    val scaleX: Float
        get() = scale.scaleX
    val scaleY: Float
        get() = scale.scaleY
    val offsetX: Float
        get() = offset.x
    val offsetY: Float
        get() = offset.y
    val originX: Float
        get() = origin.pivotFractionX
    val originY: Float
        get() = origin.pivotFractionY

    companion object {
        val Origin = Transform(
            scale = ScaleFactor(1f, 1f),
            offset = Offset.Zero,
            rotation = 0f,
            origin = TransformOrigin.TopStart,
        )
    }

    override fun toString(): String {
        return "Transform(" +
                "scale=${scale.toShortString()}, " +
                "offset=${offset.toShortString()}, " +
                "rotation=$rotation, " +
                "origin=${originX.format(2)}x${originY.format(2)}" +
                ")"
    }
}

/**
 * Linearly interpolate between two Transform.
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
@Stable
fun lerp(start: Transform, stop: Transform, fraction: Float): Transform {
    require(start.origin == stop.origin) {
        "Transform origin must be the same: start.origin=${start.origin}, stop.origin=${stop.origin}"
    }
    return start.copy(
        scale = androidx.compose.ui.layout.lerp(start.scale, stop.scale, fraction),
        offset = androidx.compose.ui.geometry.lerp(start.offset, stop.offset, fraction),
        rotation = androidx.compose.ui.util.lerp(start.rotation, stop.rotation, fraction),
    )
}

fun Transform.toShortString(): String =
    "(${scale.toShortString()},${offset.toShortString()}," +
            "$rotation,${originX.format(2)}x${originY.format(2)})"

fun Transform.times(scaleFactor: ScaleFactor): Transform {
    return this.copy(
        scale = ScaleFactor(
            scaleX = scale.scaleX * scaleFactor.scaleX,
            scaleY = scale.scaleY * scaleFactor.scaleY,
        ),
        offset = Offset(
            x = offset.x * scaleFactor.scaleX,
            y = offset.y * scaleFactor.scaleY,
        ),
    )
}

fun Transform.div(scaleFactor: ScaleFactor): Transform {
    return this.copy(
        scale = ScaleFactor(
            scaleX = scale.scaleX / scaleFactor.scaleX,
            scaleY = scale.scaleY / scaleFactor.scaleY,
        ),
        offset = Offset(
            x = offset.x / scaleFactor.scaleX,
            y = offset.y / scaleFactor.scaleY,
        ),
    )
}

fun Transform.concat(other: Transform): Transform {
    require(this.origin == other.origin) {
        "Transform origin must be the same: this.origin=${this.origin.toShortString()}, other.origin=${other.origin}"
    }
    return this.copy(
        scale = scale.times(other.scale),
        offset = (offset * other.scale) + other.offset,
        rotation = rotation + other.rotation,
    )
}