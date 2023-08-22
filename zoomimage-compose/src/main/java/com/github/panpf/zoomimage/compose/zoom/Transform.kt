package com.github.panpf.zoomimage.compose.zoom

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.isSpecified
import androidx.compose.ui.layout.isUnspecified
import com.github.panpf.zoomimage.compose.internal.Origin
import com.github.panpf.zoomimage.compose.internal.TopStart
import com.github.panpf.zoomimage.compose.internal.div
import com.github.panpf.zoomimage.compose.internal.times
import com.github.panpf.zoomimage.compose.internal.toShortString

@Immutable
data class Transform(
    val scale: ScaleFactor,
    val offset: Offset,
    val rotation: Float = 0f,
    val scaleOrigin: TransformOrigin = TransformOrigin.TopStart,
    val rotationOrigin: TransformOrigin = TransformOrigin.TopStart,
) {

    constructor(
        scaleX: Float,
        scaleY: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float = 0f,
        scaleOriginX: Float = 0f,
        scaleOriginY: Float = 0f,
        rotationOriginX: Float = 0.5f,
        rotationOriginY: Float = 0.5f,
    ) : this(
        scale = ScaleFactor(scaleX = scaleX, scaleY = scaleY),
        offset = Offset(x = offsetX, y = offsetY),
        rotation = rotation,
        scaleOrigin = TransformOrigin(
            pivotFractionX = scaleOriginX,
            pivotFractionY = scaleOriginY
        ),
        rotationOrigin = TransformOrigin(
            pivotFractionX = rotationOriginX,
            pivotFractionY = rotationOriginY
        ),
    )

    val scaleX: Float
        get() = scale.scaleX
    val scaleY: Float
        get() = scale.scaleY
    val offsetX: Float
        get() = offset.x
    val offsetY: Float
        get() = offset.y
    val scaleOriginX: Float
        get() = scaleOrigin.pivotFractionX
    val scaleOriginY: Float
        get() = scaleOrigin.pivotFractionY
    val rotationOriginX: Float
        get() = rotationOrigin.pivotFractionX
    val rotationOriginY: Float
        get() = rotationOrigin.pivotFractionY

    companion object {
        val Origin = Transform(
            scale = ScaleFactor(1f, 1f),
            offset = Offset.Zero,
            rotation = 0f,
            scaleOrigin = TransformOrigin.TopStart,
            rotationOrigin = TransformOrigin.TopStart,
        )
    }

    override fun toString(): String {
        return "Transform(" +
                "scale=${scale.toShortString()}, " +
                "offset=${offset.toShortString()}, " +
                "rotation=$rotation, " +
                "scaleOrigin=${scaleOrigin.toShortString()}, " +
                "rotationOrigin=${rotationOrigin.toShortString()}" +
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
    require(start.scale.let { it.isUnspecified || it == ScaleFactor.Origin }
            || stop.scale.let { it.isUnspecified || it == ScaleFactor.Origin }
            || start.scaleOrigin == stop.scaleOrigin
    ) {
        "When both start and stop Transform's scale are not empty, their scaleOrigin must be the same: " +
                "start.scaleOrigin=${start.scaleOrigin}, " +
                "stop.scaleOrigin=${stop.scaleOrigin}"
    }
    require(start.rotation == 0f || stop.rotation == 0f || start.rotationOrigin == stop.rotationOrigin) {
        "When both start and stop Transform's rotation are not zero, their rotationOrigin must be the same: " +
                "start.rotationOrigin=${start.rotationOrigin}, " +
                "stop.rotationOrigin=${stop.rotationOrigin}"
    }
    val scaleOrigin = if (start.scale.let { it.isSpecified && it != ScaleFactor.Origin }) {
        start.scaleOrigin
    } else if (stop.scale.let { it.isSpecified && it != ScaleFactor.Origin }) {
        stop.scaleOrigin
    } else {
        TransformOrigin.TopStart
    }
    val rotationOrigin = if (start.rotation != 0f) {
        start.rotationOrigin
    } else if (stop.rotation != 0f) {
        stop.rotationOrigin
    } else {
        TransformOrigin.TopStart
    }
    return start.copy(
        scale = androidx.compose.ui.layout.lerp(start.scale, stop.scale, fraction),
        offset = androidx.compose.ui.geometry.lerp(start.offset, stop.offset, fraction),
        rotation = androidx.compose.ui.util.lerp(start.rotation, stop.rotation, fraction),
        scaleOrigin = scaleOrigin,
        rotationOrigin = rotationOrigin,
    )
}

fun Transform.toShortString(): String =
    "(${scale.toShortString()},${offset.toShortString()},$rotation,${scaleOrigin.toShortString()},${rotationOrigin.toShortString()})"

operator fun Transform.times(scaleFactor: ScaleFactor): Transform {
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

operator fun Transform.div(scaleFactor: ScaleFactor): Transform {
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

operator fun Transform.plus(other: Transform): Transform {
    require(this.scale.let { it.isUnspecified || it == ScaleFactor.Origin }
            || other.scale.let { it.isUnspecified || it == ScaleFactor.Origin }
            || this.scaleOrigin == other.scaleOrigin
    ) {
        "When both this and other Transform's scale are not empty, their scaleOrigin must be the same: " +
                "this.scaleOrigin=${this.scaleOrigin}, " +
                "other.scaleOrigin=${other.scaleOrigin}"
    }
    require(this.rotation == 0f || other.rotation == 0f || this.rotationOrigin == other.rotationOrigin) {
        "When both this and other Transform's rotation are not zero, their rotationOrigin must be the same: " +
                "this.rotationOrigin=${this.rotationOrigin}, " +
                "other.rotationOrigin=${other.rotationOrigin}"
    }
    val scaleOrigin = if (this.scale.let { it.isSpecified && it != ScaleFactor.Origin }) {
        this.scaleOrigin
    } else if (other.scale.let { it.isSpecified && it != ScaleFactor.Origin }) {
        other.scaleOrigin
    } else {
        TransformOrigin.TopStart
    }
    val rotationOrigin = if (this.rotation != 0f) {
        this.rotationOrigin
    } else if (other.rotation != 0f) {
        other.rotationOrigin
    } else {
        TransformOrigin.TopStart
    }
    val addScale = other.scale
    return this.copy(
        scale = scale.times(addScale),
        offset = (offset * addScale) + other.offset,
        rotation = rotation + other.rotation,
        scaleOrigin = scaleOrigin,
        rotationOrigin = rotationOrigin,
    )
}

operator fun Transform.minus(other: Transform): Transform {
    require(this.scale.let { it.isUnspecified || it == ScaleFactor.Origin }
            || other.scale.let { it.isUnspecified || it == ScaleFactor.Origin }
            || this.scaleOrigin == other.scaleOrigin
    ) {
        "When both this and other Transform's scale are not empty, their scaleOrigin must be the same: " +
                "this.scaleOrigin=${this.scaleOrigin}, " +
                "other.scaleOrigin=${other.scaleOrigin}"
    }
    require(this.rotation == 0f || other.rotation == 0f || this.rotationOrigin == other.rotationOrigin) {
        "When both this and other Transform's rotation are not zero, their rotationOrigin must be the same: " +
                "this.rotationOrigin=${this.rotationOrigin}, " +
                "other.rotationOrigin=${other.rotationOrigin}"
    }
    val scaleOrigin = if (this.scale.let { it.isSpecified && it != ScaleFactor.Origin }) {
        this.scaleOrigin
    } else if (other.scale.let { it.isSpecified && it != ScaleFactor.Origin }) {
        other.scaleOrigin
    } else {
        TransformOrigin.TopStart
    }
    val rotationOrigin = if (this.rotation != 0f) {
        this.rotationOrigin
    } else if (other.rotation != 0f) {
        other.rotationOrigin
    } else {
        TransformOrigin.TopStart
    }
    val minusScale = scale.div(other.scale)
    return this.copy(
        scale = minusScale,
        offset = offset - (other.offset * minusScale),
        rotation = rotation - other.rotation,
        scaleOrigin = scaleOrigin,
        rotationOrigin = rotationOrigin,
    )
}