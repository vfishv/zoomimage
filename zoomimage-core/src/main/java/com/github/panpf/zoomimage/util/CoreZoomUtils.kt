@file:Suppress("UnnecessaryVariable")

package com.github.panpf.zoomimage.util

import com.github.panpf.zoomimage.Edge
import com.github.panpf.zoomimage.ReadMode
import com.github.panpf.zoomimage.ScrollEdge
import com.github.panpf.zoomimage.util.internal.BaseTransformHelper
import com.github.panpf.zoomimage.util.internal.format
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin


const val DefaultMediumScaleMinMultiple: Float = 3f


/* ******************************************* initial ***************************************** */

fun computeContentRotateOrigin(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    rotation: Int
): TransformOriginCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    return if (rotation != 0) {
        val center = contentSize.toSize().center
        TransformOriginCompat(
            pivotFractionX = center.x / containerSize.width,
            pivotFractionY = center.y / containerSize.height
        )
    } else {
        TransformOriginCompat.TopStart
    }
}

fun computeBaseTransform(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
): TransformCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return TransformCompat.Origin
    }
    require(rotation % 90 == 0) { "rotation must be multiple of 90" }

    val baseTransformHelper = BaseTransformHelper(
        containerSize = containerSize,
        contentSize = contentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = rotation,
    )
    return baseTransformHelper.transform
}

fun computeInitialUserTransform(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
    readMode: ReadMode?,
): TransformCompat? {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (readMode == null) return null
    if (contentScale == ContentScaleCompat.FillBounds) return null

    val baseTransformHelper = BaseTransformHelper(
        containerSize = containerSize,
        contentSize = contentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = rotation,
    )
    val rotatedContentSize = baseTransformHelper.rotatedContentSize
    if (!readMode.accept(srcSize = rotatedContentSize, dstSize = containerSize)) return null

    val widthScale = containerSize.width / rotatedContentSize.width.toFloat()
    val heightScale = containerSize.height / rotatedContentSize.height.toFloat()
    val fillScale = max(widthScale, heightScale)
    val readModeScale = ScaleFactorCompat(fillScale)

    val baseTransform = baseTransformHelper.transform
    val addScale = fillScale / baseTransform.scaleX
    val alignmentMoveToStartOffset = baseTransformHelper.alignmentOffset.let {
        OffsetCompat(it.x.coerceAtMost(0f), it.y.coerceAtMost(0f))
    }
    val readModeOffset = (alignmentMoveToStartOffset + baseTransformHelper.rotateOffset) * addScale

    val rotationOrigin = computeContentRotateOrigin(
        containerSize = containerSize,
        contentSize = contentSize,
        rotation = rotation
    )
    val readModeTransform = TransformCompat(
        scale = readModeScale,
        offset = readModeOffset,
        rotation = rotation.toFloat(),
        rotationOrigin = rotationOrigin,
    )
    val initialUserTransform = readModeTransform - baseTransformHelper.transform
    return initialUserTransform
}

fun computeStepScales(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentOriginSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    rotation: Int,
    mediumScaleMinMultiple: Float,
): FloatArray {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return floatArrayOf(1.0f, 1.0f, 1.0f)
    }

    val rotatedContentSize = contentSize.rotate(rotation)
    val baseScaleFactor = contentScale.computeScaleFactor(
        srcSize = rotatedContentSize.toSize(),
        dstSize = containerSize.toSize()
    )

    val minScale = baseScaleFactor.scaleX
    val minMediumScale = minScale * mediumScaleMinMultiple
    val mediumScale = if (contentScale != ContentScaleCompat.FillBounds) {
        // The width and height of content fill the container at the same time
        val fillContainerScale = max(
            containerSize.width / rotatedContentSize.width.toFloat(),
            containerSize.height / rotatedContentSize.height.toFloat()
        )
        // Enlarge content to the same size as its original
        val contentOriginScale = if (contentOriginSize.isNotEmpty()) {
            val rotatedContentOriginSize = contentOriginSize.rotate(rotation)
            val widthScale = rotatedContentOriginSize.width / rotatedContentSize.width.toFloat()
            val heightScale = rotatedContentOriginSize.height / rotatedContentSize.height.toFloat()
            max(widthScale, heightScale)
        } else {
            1.0f
        }
        floatArrayOf(minMediumScale, fillContainerScale, contentOriginScale).maxOrNull()!!
    } else {
        minMediumScale
    }
    val maxScale = mediumScale * 2f
    return floatArrayOf(minScale, mediumScale, maxScale)
}

fun computeInitialZoom(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentOriginSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
    readMode: ReadMode?,
    mediumScaleMinMultiple: Float,
): InitialZoom {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return InitialZoom.Origin
    }
    val stepScales = computeStepScales(
        containerSize = containerSize,
        contentSize = contentSize,
        contentOriginSize = contentOriginSize,
        contentScale = contentScale,
        rotation = rotation,
        mediumScaleMinMultiple = mediumScaleMinMultiple,
    )
    val baseTransform = computeBaseTransform(
        containerSize = containerSize,
        contentSize = contentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = rotation,
    )
    val userTransform = computeInitialUserTransform(
        containerSize = containerSize,
        contentSize = contentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = rotation,
        readMode = readMode,
    )
    return InitialZoom(
        minScale = stepScales[0],
        mediumScale = stepScales[1],
        maxScale = stepScales[2],
        baseTransform = baseTransform,
        userTransform = userTransform ?: TransformCompat.Origin
    )
}


/* ******************************************* Rect ***************************************** */

fun computeContentBaseDisplayRect(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
): RectCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return RectCompat.Zero
    }
    require(rotation % 90 == 0) { "rotation must be multiple of 90" }
    val baseTransformHelper = BaseTransformHelper(
        containerSize = containerSize,
        contentSize = contentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = rotation,
    )
    return baseTransformHelper.displayRect
}

fun computeContentBaseVisibleRect(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
): RectCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */
    // todo It can be calculated directly based on baseDisplay

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return RectCompat.Zero
    }
    val baseTransformHelper = BaseTransformHelper(
        containerSize = containerSize,
        contentSize = contentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = rotation
    )
    val scaledRotatedContentSize = baseTransformHelper.scaledRotatedContentSize

    val left: Float
    val right: Float
    val horizontalSpace = (scaledRotatedContentSize.width - containerSize.width) / 2f
    if (scaledRotatedContentSize.width.roundToInt() <= containerSize.width) {
        left = 0f
        right = scaledRotatedContentSize.width
    } else if (alignment.isStart) {
        left = 0f
        right = containerSize.width.toFloat()
    } else if (alignment.isHorizontalCenter) {
        left = horizontalSpace
        right = horizontalSpace + containerSize.width
    } else {   // alignment.isEnd
        left = scaledRotatedContentSize.width - containerSize.width
        right = scaledRotatedContentSize.width
    }

    val top: Float
    val bottom: Float
    val verticalSpace = (scaledRotatedContentSize.height - containerSize.height) / 2f
    if (scaledRotatedContentSize.height.roundToInt() <= containerSize.height) {
        top = 0f
        bottom = scaledRotatedContentSize.height
    } else if (alignment.isTop) {
        top = 0f
        bottom = containerSize.height.toFloat()
    } else if (alignment.isVerticalCenter) {
        top = verticalSpace
        bottom = verticalSpace + containerSize.height
    } else {   // alignment.isBottom
        top = scaledRotatedContentSize.height - containerSize.height
        bottom = scaledRotatedContentSize.height
    }

    val scaledRotatedContentBaseVisibleRect =
        RectCompat(left = left, top = top, right = right, bottom = bottom)
    val rotatedContentBaseVisibleRect =
        scaledRotatedContentBaseVisibleRect / baseTransformHelper.scaleFactor
    val limitedRotatedContentBaseVisibleRect =
        rotatedContentBaseVisibleRect.limitTo(baseTransformHelper.rotatedContentSize.toSize())
    val contentBaseVisibleRect =
        limitedRotatedContentBaseVisibleRect.reverseRotateInSpace(contentSize.toSize(), rotation)
    val limitedContentBaseVisibleRect = contentBaseVisibleRect.limitTo(contentSize.toSize())
    return limitedContentBaseVisibleRect
}

fun computeContentBaseInsideDisplayRect(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
): RectCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return RectCompat.Zero
    }
    require(rotation % 90 == 0) { "rotation must be multiple of 90" }
    val baseTransformHelper = BaseTransformHelper(
        containerSize = containerSize,
        contentSize = contentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = rotation,
    )
    return baseTransformHelper.insideDisplayRect
}

fun computeContentDisplayRect(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
    userScale: Float,
    userOffset: OffsetCompat
): RectCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return RectCompat.Zero
    }
    require(rotation % 90 == 0) { "rotation must be a multiple of 90, rotation: $rotation" }

    val rotatedContentSize = contentSize.rotate(rotation)
    val rotatedContentScaleFactor = contentScale.computeScaleFactor(
        srcSize = rotatedContentSize.toSize(),
        dstSize = containerSize.toSize()
    )

    val scaledRotatedContentSize = rotatedContentSize * rotatedContentScaleFactor
    val scaledRotatedContentAlignmentOffset = alignment.align(
        size = scaledRotatedContentSize,
        space = containerSize,
        ltrLayout = true,
    )

    val baseRect = IntRectCompat(scaledRotatedContentAlignmentOffset, scaledRotatedContentSize)
    val scaledBaseRect = baseRect.toRect() * userScale
    val contentDisplayRect = scaledBaseRect.translate(userOffset)
    return contentDisplayRect
}

fun computeContentVisibleRect(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
    userScale: Float,
    userOffset: OffsetCompat,
): RectCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return RectCompat.Zero
    }

    val topLeft = OffsetCompat(x = userOffset.x * -1, y = userOffset.y * -1)
    val scaledContainerVisibleRect = RectCompat(offset = topLeft, size = containerSize.toSize())
    val containerDisplayRect = scaledContainerVisibleRect / userScale

    val rotatedContentSize = contentSize.rotate(rotation)
    val rotatedContentBaseDisplayRect = computeContentBaseDisplayRect(
        containerSize = containerSize,
        contentSize = rotatedContentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = 0,
    )
    if (!containerDisplayRect.overlaps(rotatedContentBaseDisplayRect)) {
        return RectCompat.Zero
    }

    val impreciseScaledRotatedContentVisibleRect = RectCompat(
        left = (containerDisplayRect.left - rotatedContentBaseDisplayRect.left),
        top = (containerDisplayRect.top - rotatedContentBaseDisplayRect.top),
        right = (containerDisplayRect.right - rotatedContentBaseDisplayRect.left),
        bottom = (containerDisplayRect.bottom - rotatedContentBaseDisplayRect.top)
    )
    val scaledRotatedContentVisibleRect = impreciseScaledRotatedContentVisibleRect
        .limitTo(rotatedContentBaseDisplayRect.size)
    val rotatedContentScaleFactor = contentScale.computeScaleFactor(
        srcSize = rotatedContentSize.toSize(),
        dstSize = containerSize.toSize()
    )
    val rotatedContentVisibleRect = scaledRotatedContentVisibleRect / rotatedContentScaleFactor
    val contentVisibleRect =
        rotatedContentVisibleRect.reverseRotateInSpace(contentSize.toSize(), rotation)
    val limitedContentVisibleRect = contentVisibleRect.limitTo(contentSize.toSize())
    return limitedContentVisibleRect
}


/* ******************************************* Offset ***************************************** */

fun computeUserOffsetBounds(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
    userScale: Float,
    limitBaseVisibleRect: Boolean,
): RectCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return RectCompat.Zero
    }
    val rotatedContentSize = contentSize.rotate(rotation)
    val scaledContainerSize = containerSize.toSize() * userScale
    val rotatedContentBaseDisplayRect = if (limitBaseVisibleRect) {
        computeContentBaseInsideDisplayRect(
            containerSize = containerSize,
            contentSize = rotatedContentSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = 0,
        )
    } else {
        computeContentBaseDisplayRect(
            containerSize = containerSize,
            contentSize = rotatedContentSize,
            contentScale = contentScale,
            alignment = alignment,
            rotation = 0,
        )
    }
    val scaledRotatedContentBaseDisplayRect = rotatedContentBaseDisplayRect * userScale

    val horizontalBounds =
        if (scaledRotatedContentBaseDisplayRect.width.roundToInt() >= containerSize.width) {
            ((scaledRotatedContentBaseDisplayRect.right - containerSize.width) * -1)..
                    (scaledRotatedContentBaseDisplayRect.left * -1)
        } else if (alignment.isStart) {
            0f..0f
        } else if (alignment.isHorizontalCenter) {
            val horizontalSpace = (scaledContainerSize.width - containerSize.width) / 2f * -1
            horizontalSpace..horizontalSpace
        } else {   // alignment.isEnd
            val horizontalSpace = (scaledContainerSize.width - containerSize.width) * -1
            horizontalSpace..horizontalSpace
        }

    val verticalBounds =
        if (scaledRotatedContentBaseDisplayRect.height.roundToInt() >= containerSize.height) {
            ((scaledRotatedContentBaseDisplayRect.bottom - containerSize.height) * -1)..
                    (scaledRotatedContentBaseDisplayRect.top * -1)
        } else if (alignment.isTop) {
            0f..0f
        } else if (alignment.isVerticalCenter) {
            val verticalSpace = (scaledContainerSize.height - containerSize.height) / 2f * -1
            verticalSpace..verticalSpace
        } else {   // alignment.isBottom
            val verticalSpace = (scaledContainerSize.height - containerSize.height) * -1
            verticalSpace..verticalSpace
        }

    val offsetBounds = RectCompat(
        left = horizontalBounds.start,
        top = verticalBounds.start,
        right = horizontalBounds.endInclusive,
        bottom = verticalBounds.endInclusive
    )
    return offsetBounds
}

fun computeLocationUserOffset(
    containerSize: IntSizeCompat,
    containerPoint: OffsetCompat,
    userScale: Float,
): OffsetCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty()) {
        return OffsetCompat.Zero
    }
    val scaledContainerPoint = containerPoint * userScale
    val containerCenter = containerSize.center.toOffset()
    val toCenterScaledContainerPoint = scaledContainerPoint - containerCenter
    val locationOffset = toCenterScaledContainerPoint * -1f
    return locationOffset
}

fun computeScaleUserOffset(
    currentUserScale: Float,
    currentUserOffset: OffsetCompat,
    targetUserScale: Float,
    centroid: OffsetCompat,
): OffsetCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */
    return computeTransformOffset(
        currentScale = currentUserScale,
        currentOffset = currentUserOffset,
        targetScale = targetUserScale,
        centroid = centroid,
        pan = OffsetCompat.Zero,
        gestureRotate = 0f,
    )
}

fun computeTransformOffset(
    currentScale: Float,
    currentOffset: OffsetCompat,
    targetScale: Float,
    centroid: OffsetCompat,
    pan: OffsetCompat,
    gestureRotate: Float,
): OffsetCompat {
    /**
     * Rotates the given offset around the origin by the given angle in degrees.
     *
     * A positive angle indicates a counterclockwise rotation around the right-handed 2D Cartesian
     * coordinate system.
     *
     * See: [Rotation matrix](https://en.wikipedia.org/wiki/Rotation_matrix)
     */
    fun OffsetCompat.rotateBy(angle: Float): OffsetCompat {
        val angleInRadians = angle * kotlin.math.PI / 180
        return OffsetCompat(
            x = (x * cos(angleInRadians) - y * sin(angleInRadians)).toFloat(),
            y = (x * sin(angleInRadians) + y * cos(angleInRadians)).toFloat()
        )
    }

    // copied https://github.com/androidx/androidx/blob/643b1cfdd7dfbc5ccce1ad951b6999df049678b3/compose/foundation/foundation/samples/src/main/java/androidx/compose/foundation/samples/TransformGestureSamples.kt
    val oldScale = currentScale
    val newScale = targetScale
    val restoreScaleCurrentOffset = currentOffset / currentScale * -1f
    // For natural zooming and rotating, the centroid of the gesture should
    // be the fixed point where zooming and rotating occurs.
    // We compute where the centroid was (in the pre-transformed coordinate
    // space), and then compute where it will be after this delta.
    // We then compute what the new offset should be to keep the centroid
    // visually stationary for rotating and zooming, and also apply the pan.
    val targetRestoreScaleCurrentOffset =
        (restoreScaleCurrentOffset + centroid / oldScale).rotateBy(gestureRotate) - (centroid / newScale + pan / oldScale)
    val targetOffset = targetRestoreScaleCurrentOffset * newScale * -1f
    return targetOffset
}

fun computeScrollEdge(
    userOffsetBounds: RectCompat,
    userOffset: OffsetCompat,
): ScrollEdge {
    val leftFormatted = userOffsetBounds.left.roundToInt()
    val rightFormatted = userOffsetBounds.right.roundToInt()
    val xFormatted = userOffset.x.roundToInt()
    val horizontal = when {
        leftFormatted == rightFormatted -> Edge.BOTH
        xFormatted <= leftFormatted -> Edge.END
        xFormatted >= rightFormatted -> Edge.START
        else -> Edge.NONE
    }

    val topFormatted = userOffsetBounds.top.roundToInt()
    val bottomFormatted = userOffsetBounds.bottom.roundToInt()
    val yFormatted = userOffset.y.roundToInt()
    val vertical = when {
        topFormatted == bottomFormatted -> Edge.BOTH
        yFormatted <= topFormatted -> Edge.END
        yFormatted >= bottomFormatted -> Edge.START
        else -> Edge.NONE
    }
    return ScrollEdge(horizontal = horizontal, vertical = vertical)
}

/**
 * Whether you can scroll horizontally or vertical in the specified direction
 *
 * @param direction Negative to check scrolling left or up, positive to check scrolling right or down.
 */
fun canScrollByEdge(scrollEdge: ScrollEdge, horizontal: Boolean, direction: Int): Boolean {
    return if (horizontal) {
        if (direction > 0) {
            scrollEdge.horizontal != Edge.END && scrollEdge.horizontal != Edge.BOTH
        } else {
            scrollEdge.horizontal != Edge.START && scrollEdge.horizontal != Edge.BOTH
        }
    } else {
        if (direction > 0) {
            scrollEdge.vertical != Edge.END && scrollEdge.vertical != Edge.BOTH
        } else {
            scrollEdge.vertical != Edge.START && scrollEdge.vertical != Edge.BOTH
        }
    }
}


/* ******************************************* Scale ***************************************** */

fun limitScaleWithRubberBand(
    currentScale: Float,
    targetScale: Float,
    minScale: Float,
    maxScale: Float,
    rubberBandRatio: Float = 2f
): Float = when {
    targetScale > maxScale -> {
        val addScale = targetScale - currentScale
        val rubberBandMaxScale = maxScale * rubberBandRatio
        val overScale = targetScale - maxScale
        val overMaxScale = rubberBandMaxScale - maxScale
        val progress = overScale / overMaxScale
        // Multiplying by 0.5f is to be a little slower
        val limitedAddScale = addScale * (1 - progress) * 0.5f
        currentScale + limitedAddScale
    }

    targetScale < minScale -> {
        val addScale = targetScale - currentScale
        val rubberBandMinScale = minScale / rubberBandRatio
        val overScale = targetScale - minScale
        val overMinScale = rubberBandMinScale - minScale
        val progress = overScale / overMinScale
        // Multiplying by 0.5f is to be a little slower
        val limitedAddScale = addScale * (1 - progress) * 0.5f
        currentScale + limitedAddScale
    }

    else -> targetScale
}

fun calculateNextStepScale(
    stepScales: FloatArray,
    currentScale: Float,
    rangeOfError: Float = 0.1f
): Float {
    if (stepScales.isEmpty()) return currentScale
    val formattedCurrentScale = currentScale.format(1)
    return stepScales
        .find { it.format(1) > formattedCurrentScale + rangeOfError }
        ?: stepScales.first()
}


/* ******************************************* Point ***************************************** */

fun touchPointToContainerPoint(
    containerSize: IntSizeCompat,
    userScale: Float,
    userOffset: OffsetCompat,
    touchPoint: OffsetCompat
): OffsetCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty()) {
        return OffsetCompat.Zero
    }
    val scaledContainerPoint = touchPoint - userOffset
    val containerPoint = scaledContainerPoint / userScale
    return containerPoint
}

fun containerPointToTouchPoint(
    containerSize: IntSizeCompat,
    userScale: Float,
    userOffset: OffsetCompat,
    containerPoint: OffsetCompat
): OffsetCompat {
    if (containerSize.isEmpty()) {
        return OffsetCompat.Zero
    }

    val scaledContainerPoint = containerPoint * userScale
    val touchPoint = scaledContainerPoint + userOffset
    return touchPoint
}

fun containerPointToContentPoint(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
    containerPoint: OffsetCompat
): OffsetCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return OffsetCompat.Zero
    }
    val rotatedContentSize = contentSize.rotate(rotation)
    val rotatedContentBaseDisplayRect = computeContentBaseDisplayRect(
        containerSize = containerSize,
        contentSize = rotatedContentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = 0,
    )
    val scaledRotatedContentPointOffset = containerPoint - rotatedContentBaseDisplayRect.topLeft
    val rotatedContentScaleFactor = contentScale.computeScaleFactor(
        srcSize = rotatedContentSize.toSize(),
        dstSize = containerSize.toSize()
    )
    val rotatedContentPoint = (scaledRotatedContentPointOffset / rotatedContentScaleFactor)
    val limitedRotatedContentPoint = rotatedContentPoint.limitTo(rotatedContentSize.toSize())
    val contentPoint =
        limitedRotatedContentPoint.reverseRotateInSpace(contentSize.toSize(), rotation)
    return contentPoint
}

fun contentPointToContainerPoint(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
    contentPoint: OffsetCompat
): OffsetCompat {
    /*
     * Calculations are based on the following rules:
     * 1. Content is located in the top left corner of the container
     * 2. The scale center point is top left
     * 3. The rotate center point is the content center
     * 4. Apply rotation before scaling and offset
     */

    if (containerSize.isEmpty() || contentSize.isEmpty()) {
        return OffsetCompat.Zero
    }

    val rotatedContentSize = contentSize.rotate(rotation)
    val rotatedContentPoint = contentPoint.rotateInSpace(contentSize.toSize(), rotation)
    val rotatedContentScaleFactor = contentScale.computeScaleFactor(
        srcSize = rotatedContentSize.toSize(),
        dstSize = containerSize.toSize()
    )
    val scaledRotatedContentPoint = rotatedContentPoint * rotatedContentScaleFactor
    val rotatedContentBaseDisplayRect = computeContentBaseDisplayRect(
        containerSize = containerSize,
        contentSize = rotatedContentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = 0,
    )
    val containerPoint = scaledRotatedContentPoint + rotatedContentBaseDisplayRect.topLeft
    return containerPoint
}

fun touchPointToContentPoint(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
    userScale: Float,
    userOffset: OffsetCompat,
    touchPoint: OffsetCompat,
): OffsetCompat {
    val containerPoint = touchPointToContainerPoint(
        containerSize = containerSize,
        userScale = userScale,
        userOffset = userOffset,
        touchPoint = touchPoint
    )
    return containerPointToContentPoint(
        containerSize = containerSize,
        contentSize = contentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = rotation,
        containerPoint = containerPoint
    )
}

fun contentPointToTouchPoint(
    containerSize: IntSizeCompat,
    contentSize: IntSizeCompat,
    contentScale: ContentScaleCompat,
    alignment: AlignmentCompat,
    rotation: Int,
    userScale: Float,
    userOffset: OffsetCompat,
    contentPoint: OffsetCompat,
): OffsetCompat {
    val containerPoint = contentPointToContainerPoint(
        containerSize = containerSize,
        contentSize = contentSize,
        contentScale = contentScale,
        alignment = alignment,
        rotation = rotation,
        contentPoint = contentPoint
    )
    val touchPoint = containerPointToTouchPoint(
        containerSize = containerSize,
        userScale = userScale,
        userOffset = userOffset,
        containerPoint = containerPoint
    )
    return touchPoint
}