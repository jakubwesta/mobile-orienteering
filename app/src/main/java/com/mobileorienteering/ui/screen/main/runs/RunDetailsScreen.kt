package com.mobileorienteering.ui.screen.main.runs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobileorienteering.R
import com.mobileorienteering.data.model.PathPoint
import com.mobileorienteering.data.model.VisitedControlPoint
import com.mobileorienteering.ui.screen.main.map.components.RoutePathLayer
import com.mobileorienteering.ui.screen.main.map.models.Checkpoint
import com.mobileorienteering.util.calculateDistanceBetweenPoints
import com.mobileorienteering.util.calculatePaceBetweenInstants
import com.mobileorienteering.util.formatDate
import com.mobileorienteering.util.formatDistance
import com.mobileorienteering.util.formatDuration
import com.mobileorienteering.util.formatDurationFromInstants
import com.mobileorienteering.util.formatTime
import com.mobileorienteering.util.calculatePace
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.map.*
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDetailsScreen(
    activityId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val activity by viewModel.getActivity(activityId).collectAsState(initial = null)
    val map by viewModel.getMapForActivity(activityId).collectAsState(initial = null)
    val isLoading by remember { viewModel.isLoading }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Run Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading || activity == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Nagłówek
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            activity!!.title,
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                formatDate(activity!!.startTime),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatTime(activity!!.startTime),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mapa z trasą
                    if (activity!!.pathData.isNotEmpty()) {
                        RunMapPreview(
                            pathData = activity!!.pathData,
                            checkpoints = map?.controlPoints?.mapIndexed { index, cp ->
                                Checkpoint(
                                    position = Position(cp.longitude, cp.latitude),
                                    name = "Punkt ${index + 1}"
                                )
                            } ?: emptyList(),
                            visitedIndices = activity!!.visitedControlPoints.map { it.order - 1 }.toSet(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    // Statystyki ogólne
                    RunStatsCard(
                        distance = activity!!.distance,
                        duration = activity!!.duration,
                        startTime = activity!!.startTime,
                        pathData = activity!!.pathData,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Timeline z checkpointami
                    val visitedControlPoints = activity!!.visitedControlPoints.sortedBy { it.visitedAt }

                    if (visitedControlPoints.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Text(
                            "Splits",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        RunTimeline(
                            startTime = activity!!.startTime,
                            visitedPoints = visitedControlPoints,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else if (activity!!.totalControlPoints > 0) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Text(
                            "No control points visited",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RunMapPreview(
    pathData: List<PathPoint>,
    checkpoints: List<Checkpoint>,
    visitedIndices: Set<Int>,
    modifier: Modifier = Modifier
) {
    val cameraState = rememberCameraState()
    val styleState = rememberStyleState()

    // Oblicz centrum i zoom na podstawie trasy
    LaunchedEffect(pathData) {
        if (pathData.isNotEmpty()) {
            val minLat = pathData.minOf { it.latitude }
            val maxLat = pathData.maxOf { it.latitude }
            val minLon = pathData.minOf { it.longitude }
            val maxLon = pathData.maxOf { it.longitude }

            val centerLat = (minLat + maxLat) / 2
            val centerLon = (minLon + maxLon) / 2

            // Oblicz zoom na podstawie rozmiaru bounding box
            val latDiff = maxLat - minLat
            val lonDiff = maxLon - minLon
            val maxDiff = maxOf(latDiff, lonDiff)

            val zoom = when {
                maxDiff > 0.1 -> 11.0
                maxDiff > 0.05 -> 12.0
                maxDiff > 0.02 -> 13.0
                maxDiff > 0.01 -> 14.0
                maxDiff > 0.005 -> 15.0
                else -> 16.0
            }

            cameraState.position = CameraPosition(
                target = Position(centerLon, centerLat),
                zoom = zoom
            )
        }
    }

    MaplibreMap(
        modifier = modifier,
        cameraState = cameraState,
        styleState = styleState,
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        options = MapOptions(
            ornamentOptions = OrnamentOptions.AllDisabled,
            gestureOptions = GestureOptions.Standard
        )
    ) {
        // Rysuj trasę
        RoutePathLayer(
            pathData = pathData,
            color = Color(0xFF4CAF50),
            width = 4f
        )

        // Rysuj checkpointy
        checkpoints.forEachIndexed { index, checkpoint ->
            val isVisited = index in visitedIndices
            val feature = Feature(
                geometry = Point(Position(checkpoint.position.longitude, checkpoint.position.latitude)),
                properties = null
            )
            val source = rememberGeoJsonSource(data = GeoJsonData.Features(feature))

            CircleLayer(
                id = "checkpoint-$index",
                source = source,
                color = const(if (isVisited) Color(0xFF4CAF50) else Color(0xFF9E9E9E)),
                radius = const(10.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(2.dp)
            )
        }

        // Punkt startowy
        if (pathData.isNotEmpty()) {
            val startPoint = pathData.first()
            val startFeature = Feature(
                geometry = Point(Position(startPoint.longitude, startPoint.latitude)),
                properties = null
            )
            val startSource = rememberGeoJsonSource(data = GeoJsonData.Features(startFeature))

            CircleLayer(
                id = "start-point",
                source = startSource,
                color = const(Color(0xFF2196F3)),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(3.dp)
            )
        }

        // Punkt końcowy
        if (pathData.size > 1) {
            val endPoint = pathData.last()
            val endFeature = Feature(
                geometry = Point(Position(endPoint.longitude, endPoint.latitude)),
                properties = null
            )
            val endSource = rememberGeoJsonSource(data = GeoJsonData.Features(endFeature))

            CircleLayer(
                id = "end-point",
                source = endSource,
                color = const(Color(0xFFF44336)),
                radius = const(8.dp),
                strokeColor = const(Color.White),
                strokeWidth = const(3.dp)
            )
        }
    }
}

@Composable
private fun RunStatsCard(
    distance: Double,
    duration: String,
    startTime: Instant,
    pathData: List<PathPoint>,
    modifier: Modifier = Modifier
) {
    // Oblicz czas trwania w sekundach
    val durationSeconds = try {
        Duration.parse(duration).seconds
    } catch (e: Exception) {
        // Próba parsowania formatu "MM:SS" lub "HH:MM:SS"
        val parts = duration.split(":")
        when (parts.size) {
            2 -> parts[0].toLongOrNull()?.times(60)?.plus(parts[1].toLongOrNull() ?: 0) ?: 0L
            3 -> parts[0].toLongOrNull()?.times(3600)
                ?.plus(parts[1].toLongOrNull()?.times(60) ?: 0)
                ?.plus(parts[2].toLongOrNull() ?: 0) ?: 0L
            else -> 0L
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Dystans
            StatItem(
                label = "Distance",
                value = formatDistance(distance),
                icon = R.drawable.ic_route
            )

            // Czas
            StatItem(
                label = "Duration",
                value = formatDuration(duration),
                icon = R.drawable.ic_runs_outlined
            )

            // Tempo
            StatItem(
                label = "Pace",
                value = calculatePace(distance, durationSeconds),
                icon = R.drawable.ic_play_arrow
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RunTimeline(
    startTime: Instant,
    visitedPoints: List<VisitedControlPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val pointRadius = 10.dp
    val lineWidth = 3.dp

    Column(modifier = modifier.fillMaxWidth()) {
        visitedPoints.forEachIndexed { index, visitedPoint ->
            val isFirst = index == 0
            val isLast = index == visitedPoints.lastIndex

            val previousTime = if (isFirst) startTime else visitedPoints[index - 1].visitedAt
            val intervalDuration = formatDurationFromInstants(previousTime, visitedPoint.visitedAt)
            val totalTime = formatDurationFromInstants(startTime, visitedPoint.visitedAt)

            val distanceFromPrevious = if (isFirst) {
                null
            } else {
                val prev = visitedPoints[index - 1]
                calculateDistanceBetweenPoints(
                    prev.latitude, prev.longitude,
                    visitedPoint.latitude, visitedPoint.longitude
                )
            }

            val totalDistance = if (isFirst) {
                0.0
            } else {
                var sum = 0.0
                for (i in 1..index) {
                    val prevCp = visitedPoints[i - 1]
                    val currCp = visitedPoints[i]
                    sum += calculateDistanceBetweenPoints(
                        prevCp.latitude, prevCp.longitude,
                        currCp.latitude, currCp.longitude
                    )
                }
                sum
            }

            // Oblicz tempo dla tego odcinka
            val segmentPace = if (distanceFromPrevious != null && distanceFromPrevious > 0) {
                calculatePaceBetweenInstants(distanceFromPrevious, previousTime, visitedPoint.visitedAt)
            } else {
                null
            }

            val checkpointLabel = when {
                isFirst -> "Start"
                isLast -> "Finish"
                else -> visitedPoint.controlPointName.ifEmpty { "Control Point ${visitedPoint.order}" }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top
            ) {
                // Timeline line with point
                Box(
                    modifier = Modifier
                        .width(pointRadius * 2 + 4.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        val pointRadiusPx = pointRadius.toPx()
                        val lineWidthPx = lineWidth.toPx()

                        if (!isFirst) {
                            drawLine(
                                color = lineColor,
                                start = Offset(centerX, 0f),
                                end = Offset(centerX, pointRadiusPx),
                                strokeWidth = lineWidthPx
                            )
                        }

                        if (!isLast) {
                            drawLine(
                                color = lineColor,
                                start = Offset(centerX, pointRadiusPx * 2),
                                end = Offset(centerX, size.height),
                                strokeWidth = lineWidthPx
                            )
                        }

                        drawCircle(
                            color = pointColor,
                            radius = pointRadiusPx,
                            center = Offset(centerX, pointRadiusPx)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Checkpoint info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = checkpointLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Interval, Distance, Pace
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Interval:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = intervalDuration,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (distanceFromPrevious != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Dist:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDistance(distanceFromPrevious),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // Pace dla tego odcinka
                    if (segmentPace != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Pace:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = segmentPace,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    // Total Time and Distance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = totalTime,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Total Dist:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDistance(totalDistance),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
