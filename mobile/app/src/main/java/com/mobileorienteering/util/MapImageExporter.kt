package com.mobileorienteering.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import com.mobileorienteering.data.model.app.Checkpoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.pow
import androidx.core.graphics.createBitmap

@Singleton
class MapImageExporter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    suspend fun exportMapImage(
        checkpoints: List<Checkpoint>,
        styleUrl: String,
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT
    ): ByteArray {
        require(checkpoints.isNotEmpty()) { "Cannot export map image without control points" }

        val snapshot = withTimeout(SNAPSHOT_TIMEOUT_MS) {
            renderMapSnapshot(checkpoints, styleUrl, width, height)
        }

        return try {
            withContext(Dispatchers.IO) {
                ByteArrayOutputStream().use { output ->
                    snapshot.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                    output.toByteArray()
                }
            }
        } finally {
            snapshot.recycle()
        }
    }

    private suspend fun renderMapSnapshot(
        checkpoints: List<Checkpoint>,
        styleUrl: String,
        width: Int,
        height: Int
    ): Bitmap {
        val camera = checkpoints
            .map { it.position.latitude to it.position.longitude }
            .let { fittedSnapshotCamera(it, width, height) }

        val bitmap = if (width > MAX_SINGLE_SNAPSHOT_WIDTH || height > MAX_SINGLE_SNAPSHOT_HEIGHT) {
            renderTiledMapSnapshot(styleUrl, width, height, camera)
        } else {
            renderBaseMapSnapshot(styleUrl, width, height, camera)
        }

        drawCourseOverlay(Canvas(bitmap), checkpoints) { checkpoint ->
            pixelForLatLon(
                latitude = checkpoint.position.latitude,
                longitude = checkpoint.position.longitude,
                camera = camera,
                width = width,
                height = height
            )
        }

        return bitmap
    }

    private suspend fun renderTiledMapSnapshot(
        styleUrl: String,
        width: Int,
        height: Int,
        camera: SnapshotCamera
    ): Bitmap {
        val output = createBitmap(width, height)
        val canvas = Canvas(output)
        val tilePaint = Paint().apply {
            isFilterBitmap = false
            isAntiAlias = false
        }

        var top = 0
        while (top < height) {
            val targetHeight = min(MAX_TILE_SIZE, height - top)
            var left = 0

            while (left < width) {
                val targetWidth = min(MAX_TILE_SIZE, width - left)
                val overlapLeft = min(TILE_OVERLAP_PX, left)
                val overlapTop = min(TILE_OVERLAP_PX, top)
                val overlapRight = min(TILE_OVERLAP_PX, width - left - targetWidth)
                val overlapBottom = min(TILE_OVERLAP_PX, height - top - targetHeight)
                val renderWidth = targetWidth + overlapLeft + overlapRight
                val renderHeight = targetHeight + overlapTop + overlapBottom
                val renderCenterX = left - overlapLeft + renderWidth / 2.0
                val renderCenterY = top - overlapTop + renderHeight / 2.0
                val offsetX = renderCenterX - width / 2.0
                val offsetY = renderCenterY - height / 2.0
                val tileCamera = camera.shiftedByPixels(offsetX, offsetY)
                val tile = renderBaseMapSnapshot(styleUrl, renderWidth, renderHeight, tileCamera)

                canvas.drawBitmap(
                    tile,
                    Rect(overlapLeft, overlapTop, overlapLeft + targetWidth, overlapTop + targetHeight),
                    Rect(left, top, left + targetWidth, top + targetHeight),
                    tilePaint
                )
                tile.recycle()
                left += targetWidth
            }

            top += targetHeight
        }

        return output
    }

    private suspend fun renderBaseMapSnapshot(
        styleUrl: String,
        width: Int,
        height: Int,
        camera: SnapshotCamera
    ): Bitmap = suspendCancellableCoroutine { continuation ->
        val snapshotter = MapSnapshotter(
            context,
            MapSnapshotter.Options(width, height)
                .withStyleBuilder(Style.Builder().fromUri(styleUrl))
                .apply {
                    showLogo = false
                    showAttribution = false
                }
        )
        snapshotter.setCameraPosition(
            CameraPosition.Builder()
                .target(LatLng(camera.centerLat, camera.centerLon))
                .zoom(camera.zoom)
                .build()
        )

        snapshotter.start(
            object : MapSnapshotter.SnapshotReadyCallback {
                override fun onSnapshotReady(snapshot: MapSnapshot) {
                    if (!continuation.isActive) return

                    try {
                        val bitmap = snapshot.bitmap.takeIf { it.isMutable }
                            ?: snapshot.bitmap.copy(Bitmap.Config.ARGB_8888, true)
                            ?: throw OutOfMemoryError("Could not allocate mutable map snapshot bitmap")

                        continuation.resume(bitmap)
                    } catch (error: Throwable) {
                        continuation.resumeWithException(error)
                    }
                }
            },
            object : MapSnapshotter.ErrorHandler {
                override fun onError(error: String) {
                    if (!continuation.isActive) return
                    continuation.resumeWithException(Exception(error))
                }
            }
        )

        continuation.invokeOnCancellation {
            snapshotter.cancel()
        }
    }

    private fun SnapshotCamera.shiftedByPixels(offsetX: Double, offsetY: Double): SnapshotCamera {
        val scale = TILE_SIZE * 2.0.pow(zoom)
        val shiftedWorldX = longitudeToWorldX(centerLon) + offsetX / scale
        val shiftedWorldY = latitudeToWorldY(centerLat) + offsetY / scale

        return SnapshotCamera(
            centerLat = worldYToLatitude(shiftedWorldY),
            centerLon = worldXToLongitude(shiftedWorldX),
            zoom = zoom
        )
    }

    private fun pixelForLatLon(
        latitude: Double,
        longitude: Double,
        camera: SnapshotCamera,
        width: Int,
        height: Int
    ): PointF {
        val scale = TILE_SIZE * 2.0.pow(camera.zoom)
        val centerX = longitudeToWorldX(camera.centerLon) * scale
        val centerY = latitudeToWorldY(camera.centerLat) * scale
        val pointX = longitudeToWorldX(longitude) * scale
        val pointY = latitudeToWorldY(latitude) * scale

        return PointF(
            (width / 2.0 + pointX - centerX).toFloat(),
            (height / 2.0 + pointY - centerY).toFloat()
        )
    }

    private fun drawCourseOverlay(
        canvas: Canvas,
        checkpoints: List<Checkpoint>,
        pointForCheckpoint: (Checkpoint) -> PointF
    ) {
        val points = checkpoints.map(pointForCheckpoint)

        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COURSE_COLOR
            strokeWidth = COURSE_WIDTH_PX
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        points.zipWithNext().forEach { (start, end) ->
            val trimmed = trimSegment(start, end, MARKER_RADIUS_PX + 4f)
            if (trimmed != null) {
                canvas.drawLine(trimmed.first.x, trimmed.first.y, trimmed.second.x, trimmed.second.y, routePaint)
            }
        }

        points.forEachIndexed { index, point ->
            drawCheckpoint(canvas, point, index + 1)
        }
    }

    private fun drawCheckpoint(canvas: Canvas, point: PointF, number: Int) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COURSE_COLOR
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MARKER_STROKE_COLOR
            strokeWidth = MARKER_STROKE_WIDTH_PX
            style = Paint.Style.STROKE
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = MARKER_TEXT_SIZE_PX
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        canvas.drawCircle(point.x, point.y, MARKER_RADIUS_PX, fillPaint)
        canvas.drawCircle(point.x, point.y, MARKER_RADIUS_PX, strokePaint)

        val textY = point.y - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(number.toString(), point.x, textY, textPaint)
    }

    private fun trimSegment(start: PointF, end: PointF, trimPx: Float): Pair<PointF, PointF>? {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = hypot(dx, dy)
        if (length <= trimPx * 2f + 1f) return null

        val ux = dx / length
        val uy = dy / length
        return PointF(start.x + ux * trimPx, start.y + uy * trimPx) to
            PointF(end.x - ux * trimPx, end.y - uy * trimPx)
    }

    private fun fittedSnapshotCamera(
        latLons: List<Pair<Double, Double>>,
        width: Int,
        height: Int
    ): SnapshotCamera {
        if (latLons.isEmpty()) return SnapshotCamera(0.0, 0.0, DEFAULT_ZOOM)
        if (latLons.size == 1) return SnapshotCamera(latLons.first().first, latLons.first().second, SINGLE_POINT_ZOOM)

        val minLat = latLons.minOf { it.first }.coerceIn(MIN_MERCATOR_LAT, MAX_MERCATOR_LAT)
        val maxLat = latLons.maxOf { it.first }.coerceIn(MIN_MERCATOR_LAT, MAX_MERCATOR_LAT)
        val minLon = latLons.minOf { it.second }
        val maxLon = latLons.maxOf { it.second }

        val usableWidth = (width * (1f - EXPORT_BORDER_FRACTION * 2f)).coerceAtLeast(1f)
        val usableHeight = (height * (1f - EXPORT_BORDER_FRACTION * 2f)).coerceAtLeast(1f)

        val xSpan = (longitudeToWorldX(maxLon) - longitudeToWorldX(minLon)).coerceAtLeast(MIN_WORLD_SPAN)
        val ySpan = (latitudeToWorldY(minLat) - latitudeToWorldY(maxLat)).coerceAtLeast(MIN_WORLD_SPAN)

        val zoomForWidth = log2(usableWidth / (TILE_SIZE * xSpan))
        val zoomForHeight = log2(usableHeight / (TILE_SIZE * ySpan))
        val zoom = min(zoomForWidth, zoomForHeight).coerceIn(MIN_ZOOM, MAX_ZOOM)

        val centerLon = worldXToLongitude((longitudeToWorldX(minLon) + longitudeToWorldX(maxLon)) / 2.0)
        val centerLat = worldYToLatitude((latitudeToWorldY(minLat) + latitudeToWorldY(maxLat)) / 2.0)

        return SnapshotCamera(centerLat, centerLon, zoom)
    }

    private fun longitudeToWorldX(longitude: Double): Double = (longitude + 180.0) / 360.0

    private fun worldXToLongitude(x: Double): Double = x * 360.0 - 180.0

    private fun latitudeToWorldY(latitude: Double): Double {
        val sinLatitude = kotlin.math.sin(latitude.coerceIn(MIN_MERCATOR_LAT, MAX_MERCATOR_LAT) * DEG_TO_RAD)
        return 0.5 - ln((1.0 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * Math.PI)
    }

    private fun worldYToLatitude(y: Double): Double {
        val n = Math.PI - 2.0 * Math.PI * y
        return RAD_TO_DEG * kotlin.math.atan(0.5 * (kotlin.math.exp(n) - kotlin.math.exp(-n)))
    }

    private data class SnapshotCamera(
        val centerLat: Double,
        val centerLon: Double,
        val zoom: Double
    )

    private companion object {
        const val DEFAULT_WIDTH = 2048
        const val DEFAULT_HEIGHT = 1152
        const val MAX_SINGLE_SNAPSHOT_WIDTH = 4096
        const val MAX_SINGLE_SNAPSHOT_HEIGHT = 4096
        const val MAX_TILE_SIZE = 2048
        const val TILE_OVERLAP_PX = 128
        const val SNAPSHOT_TIMEOUT_MS = 60_000L
        const val TILE_SIZE = 512.0
        const val EXPORT_BORDER_FRACTION = 0.05f
        const val MIN_WORLD_SPAN = 1e-9
        const val MIN_ZOOM = 1.0
        const val MAX_ZOOM = 18.0
        const val DEFAULT_ZOOM = 10.0
        const val SINGLE_POINT_ZOOM = 17.0
        const val MIN_MERCATOR_LAT = -85.05112878
        const val MAX_MERCATOR_LAT = 85.05112878
        const val DEG_TO_RAD = Math.PI / 180.0
        const val RAD_TO_DEG = 180.0 / Math.PI
        const val JPEG_QUALITY = 92
        const val COURSE_COLOR = 0xFFFF6A3D.toInt()
        const val MARKER_STROKE_COLOR = 0xFF020202.toInt()
        const val COURSE_WIDTH_PX = 10f
        const val MARKER_RADIUS_PX = 26f
        const val MARKER_STROKE_WIDTH_PX = 7f
        const val MARKER_TEXT_SIZE_PX = 34f
        const val TAG = "MapImageExporter"
    }
}
