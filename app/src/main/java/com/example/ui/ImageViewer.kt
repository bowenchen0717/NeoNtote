package com.example.ui

import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

fun saveUriToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val imagesDir = File(context.filesDir, "note_images").apply { if (!exists()) mkdirs() }
        val file = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        "file://${file.absolutePath}"
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


@Composable
fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    FullScreenImageViewer(
        imageUrls = listOf(imageUrl),
        initialIndex = 0,
        onDismiss = onDismiss
    )
}

@Composable
fun FullScreenImageViewer(
    imageUrls: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    if (imageUrls.isEmpty()) return
    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, imageUrls.size - 1)) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val currentUrl = imageUrls.getOrNull(currentIndex) ?: imageUrls.first()

    LaunchedEffect(currentIndex) {
        scale = 1f
        offset = Offset.Zero
    }

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = currentUrl,
                contentDescription = "Full Screen Image",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )

            // Previous / Next Buttons for Multiple Images
            if (imageUrls.size > 1) {
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { currentIndex -= 1 },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                if (currentIndex < imageUrls.size - 1) {
                    IconButton(
                        onClick = { currentIndex += 1 },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Counter Badge at Bottom
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${imageUrls.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Top Header Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Zoom",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
