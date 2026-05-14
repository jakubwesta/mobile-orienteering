package com.mobileorienteering.ui.screens.library.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mobileorienteering.R
import com.mobileorienteering.data.model.domain.Map
import com.mobileorienteering.ui.core.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlin.math.max

private const val MapPreviewWidthFraction = 0.88f

@Composable
fun MapCard(
    map: Map,
    onEdit: () -> Unit,
    onChangeImage: () -> Unit,
    onDelete: () -> Unit,
    onStartRun: () -> Unit,
    onImageClick: (ImageBitmap) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        map.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Strings.Plurals.ControlPointCount(map.controlPoints.size, map.controlPoints.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_menu_dots_vertical),
                            contentDescription = Strings.Accessibility.moreOptions,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(Strings.Action.edit) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit),
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.library_change_image)) },
                            onClick = {
                                showMenu = false
                                onChangeImage()
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_map_outlined),
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(Strings.Action.delete) },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_trash_filled),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            MapImagePreview(map, onImageClick)

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onStartRun,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(Strings.Runs.startRun)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(Strings.Map.deleteMapTitle) },
            text = { Text(Strings.Formatted.libraryDeleteMapMessage(context, map.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(Strings.Action.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(Strings.Action.cancel)
                }
            }
        )
    }
}

@Composable
private fun MapImagePreview(map: Map, onImageClick: (ImageBitmap) -> Unit) {
    val imageSource = map.localImagePath ?: map.imageUrl
    var imageBitmap by remember(imageSource) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(map.localImagePath, map.imageUrl) {
        imageBitmap = withContext(Dispatchers.IO) {
            loadMapImage(map.localImagePath, map.imageUrl)
        }
    }

    imageBitmap?.let { bitmap ->
        val aspectRatio = if (bitmap.height > 0) {
            bitmap.width.toFloat() / bitmap.height.toFloat()
        } else {
            1f
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.library_change_image),
                modifier = Modifier
                    .fillMaxWidth(MapPreviewWidthFraction)
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onImageClick(bitmap) },
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun loadMapImage(localImagePath: String?, imageUrl: String?): ImageBitmap? {
    return try {
        val localFile = localImagePath?.let(::File)
        val bitmap = if (localFile?.exists() == true) {
            decodeSampledFile(localFile)
        } else if (imageUrl != null) {
            val bytes = URL(imageUrl).openStream().use { it.readBytes() }
            decodeSampledBytes(bytes)
        } else {
            null
        }

        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

private fun decodeSampledFile(file: File) = BitmapFactory.Options()
    .apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, this)
        inSampleSize = calculateInSampleSize(outWidth, outHeight)
        inJustDecodeBounds = false
    }
    .let { options -> BitmapFactory.decodeFile(file.absolutePath, options) }

private fun decodeSampledBytes(bytes: ByteArray) = BitmapFactory.Options()
    .apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
        inSampleSize = calculateInSampleSize(outWidth, outHeight)
        inJustDecodeBounds = false
    }
    .let { options -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }

private fun calculateInSampleSize(width: Int, height: Int): Int {
    val maxDimension = max(width, height)
    var sampleSize = 1

    while (maxDimension / sampleSize > 1600) {
        sampleSize *= 2
    }

    return sampleSize
}
