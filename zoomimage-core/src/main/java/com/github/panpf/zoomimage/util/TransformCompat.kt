package com.github.panpf.zoomimage.util

data class TransformCompat(
    val scale: ScaleFactorCompat,
    val offset: OffsetCompat,
    val rotation: Float = 0f,
    val scaleOrigin: TransformOriginCompat = TransformOriginCompat.TopStart,
    val rotationOrigin: TransformOriginCompat = TransformOriginCompat.Center,
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
        scale = ScaleFactorCompat(scaleX = scaleX, scaleY = scaleY),
        offset = OffsetCompat(x = offsetX, y = offsetY),
        rotation = rotation,
        scaleOrigin = TransformOriginCompat(
            pivotFractionX = scaleOriginX,
            pivotFractionY = scaleOriginY
        ),
        rotationOrigin = TransformOriginCompat(
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
        val Origin = TransformCompat(
            scale = ScaleFactorCompat(1f, 1f),
            offset = OffsetCompat.Zero,
            rotation = 0f,
            scaleOrigin = TransformOriginCompat.TopStart,
            rotationOrigin = TransformOriginCompat.Center,
        )
    }

    override fun toString(): String {
        return "TransformCompat(" +
                "scale=${scale.toShortString()}, " +
                "offset=${offset.toShortString()}, " +
                "rotation=$rotation, " +
                "scaleOrigin=${scaleOrigin.toShortString()}, " +
                "rotationOrigin=${rotationOrigin.toShortString()}" +
                ")"
    }
}

/**
 * Linearly interpolate between two TransformCompat.
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
fun lerp(start: TransformCompat, stop: TransformCompat, fraction: Float): TransformCompat {
    require(start.scaleOrigin == stop.scaleOrigin && start.rotationOrigin == stop.rotationOrigin) {
        "Transform scaleOrigin and rotationOrigin must be the same: " +
                "start.scaleOrigin=${start.scaleOrigin}, " +
                "stop.scaleOrigin=${stop.scaleOrigin}, " +
                "start.rotationOrigin=${start.rotationOrigin}, " +
                "stop.rotationOrigin=${stop.rotationOrigin}"
    }
    return start.copy(
        scale = lerp(start.scale, stop.scale, fraction),
        offset = lerp(start.offset, stop.offset, fraction),
        rotation = com.github.panpf.zoomimage.util.internal.lerp(
            start.rotation,
            stop.rotation,
            fraction
        ),
    )
}

fun TransformCompat.toShortString(): String =
    "(${scale.toShortString()},${offset.toShortString()},$rotation,${scaleOrigin.toShortString()},${rotationOrigin.toShortString()})"

fun TransformCompat.times(scaleFactor: ScaleFactorCompat): TransformCompat {
    return this.copy(
        scale = ScaleFactorCompat(
            scaleX = scale.scaleX * scaleFactor.scaleX,
            scaleY = scale.scaleY * scaleFactor.scaleY,
        ),
        offset = OffsetCompat(
            x = offset.x * scaleFactor.scaleX,
            y = offset.y * scaleFactor.scaleY,
        ),
    )
}

fun TransformCompat.div(scaleFactor: ScaleFactorCompat): TransformCompat {
    return this.copy(
        scale = ScaleFactorCompat(
            scaleX = scale.scaleX / scaleFactor.scaleX,
            scaleY = scale.scaleY / scaleFactor.scaleY,
        ),
        offset = OffsetCompat(
            x = offset.x / scaleFactor.scaleX,
            y = offset.y / scaleFactor.scaleY,
        ),
    )
}

fun TransformCompat.concat(other: TransformCompat): TransformCompat {
    require(this.scaleOrigin == other.scaleOrigin && this.rotationOrigin == other.rotationOrigin) {
        "Transform scaleOrigin and rotationOrigin must be the same: " +
                "this.scaleOrigin=${this.scaleOrigin}, " +
                "other.scaleOrigin=${other.scaleOrigin}, " +
                "this.rotationOrigin=${this.rotationOrigin}, " +
                "other.rotationOrigin=${other.rotationOrigin}"
    }
    val addScale = other.scale
    return this.copy(
        scale = scale.times(addScale),
        offset = (offset * addScale) + other.offset,
        rotation = rotation + other.rotation,
    )
}

fun TransformCompat.split(other: TransformCompat): TransformCompat {
    require(this.scaleOrigin == other.scaleOrigin && this.rotationOrigin == other.rotationOrigin) {
        "Transform scaleOrigin and rotationOrigin must be the same: " +
                "this.scaleOrigin=${this.scaleOrigin}, " +
                "other.scaleOrigin=${other.scaleOrigin}, " +
                "this.rotationOrigin=${this.rotationOrigin}, " +
                "other.rotationOrigin=${other.rotationOrigin}"
    }
    val minusScale = scale.div(other.scale)
    return this.copy(
        scale = minusScale,
        offset = offset - (other.offset * minusScale),
        rotation = rotation - other.rotation,
    )
}