package com.grinch.rivo4.view.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.provider.ContactsContract
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.grinch.rivo4.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun AvatarCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropSuccess: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var userScale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var activeViewportDiameterPx by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(imageUri) {
        isLoading = true
        val loaded = loadOrientedBitmap(context, imageUri)
        if (loaded != null) {
            sourceBitmap = loaded
            isLoading = false
        } else {
            isLoading = false
            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isSaving,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                    }

                    Text(
                        text = stringResource(R.string.crop_avatar_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = {
                                rotationDegrees = (rotationDegrees + 90f) % 360f
                            },
                            enabled = !isSaving && sourceBitmap != null,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = stringResource(R.string.action_rotate))
                        }

                        FilledTonalIconButton(
                            onClick = {
                                userScale = 1f
                                offset = Offset.Zero
                                rotationDegrees = 0f
                            },
                            enabled = !isSaving && sourceBitmap != null,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.White.copy(alpha = 0.15f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_reset))
                        }
                    }
                }

                // Viewport Area
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    } else if (sourceBitmap != null) {
                        val bmp = sourceBitmap!!
                        val imageBitmap = remember(bmp) { bmp.asImageBitmap() }

                        val minDim = minOf(maxWidth, maxHeight)
                        val viewportDiameter = minOf(minDim * 0.78f, 320.dp)
                        val viewportDiameterPx = with(LocalDensity.current) { viewportDiameter.toPx() }
                        activeViewportDiameterPx = viewportDiameterPx
                        val viewportRadiusPx = viewportDiameterPx / 2f

                        val isSideways = (rotationDegrees.toInt() % 180 != 0)
                        val effectiveW = if (isSideways) bmp.height else bmp.width
                        val effectiveH = if (isSideways) bmp.width else bmp.height
                        val baseScale = maxOf(
                            viewportDiameterPx / effectiveW.toFloat(),
                            viewportDiameterPx / effectiveH.toFloat()
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        if (!isSaving) {
                                            userScale = (userScale * zoom).coerceIn(0.5f, 5f)
                                            offset += pan
                                        }
                                    }
                                }
                        ) {
                            val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                            val totalScale = baseScale * userScale

                            // Draw image with transforms
                            clipRect {
                                withTransform({
                                    translate(canvasCenter.x + offset.x, canvasCenter.y + offset.y)
                                    rotate(rotationDegrees, Offset.Zero)
                                    scale(totalScale, totalScale, Offset.Zero)
                                    translate(-bmp.width / 2f, -bmp.height / 2f)
                                }) {
                                    drawImage(image = imageBitmap)
                                }
                            }

                            // Circular cutout scrim
                            val scrimPath = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, size.width, size.height))
                                addOval(Rect(center = canvasCenter, radius = viewportRadiusPx))
                            }
                            drawPath(scrimPath, color = Color.Black.copy(alpha = 0.7f))

                            // Circular outline ring
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = viewportRadiusPx,
                                center = canvasCenter,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }

                    Button(
                        onClick = {
                            val bmp = sourceBitmap ?: return@Button
                            isSaving = true
                            scope.launch {
                                val croppedUri = cropAndSaveBitmap(
                                    context = context,
                                    source = bmp,
                                    userScale = userScale,
                                    offset = offset,
                                    rotationDegrees = rotationDegrees,
                                    viewportDiameterPx = if (activeViewportDiameterPx > 0f) activeViewportDiameterPx else 320f
                                )
                                isSaving = false
                                if (croppedUri != null) {
                                    onCropSuccess(croppedUri)
                                } else {
                                    Toast.makeText(context, "Failed to crop avatar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSaving && sourceBitmap != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_crop))
                        }
                    }
                }
            }
        }
    }
}

private suspend fun cropAndSaveBitmap(
    context: Context,
    source: Bitmap,
    userScale: Float,
    offset: Offset,
    rotationDegrees: Float,
    viewportDiameterPx: Float,
    targetSize: Int = 720
): Uri? = withContext(Dispatchers.IO) {
    try {
        val isSideways = (rotationDegrees.toInt() % 180 != 0)
        val effectiveW = if (isSideways) source.height else source.width
        val effectiveH = if (isSideways) source.width else source.height

        val outputBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outputBitmap)

        val baseScale = maxOf(
            targetSize / effectiveW.toFloat(),
            targetSize / effectiveH.toFloat()
        )
        val totalScale = baseScale * userScale

        val matrix = Matrix()
        matrix.postTranslate(-source.width / 2f, -source.height / 2f)
        matrix.postScale(totalScale, totalScale)
        matrix.postRotate(rotationDegrees)
        matrix.postTranslate(
            targetSize / 2f + offset.x * (targetSize.toFloat() / viewportDiameterPx),
            targetSize / 2f + offset.y * (targetSize.toFloat() / viewportDiameterPx)
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
        // Clean up excess older avatar files
        avatarsDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(20)?.forEach { it.delete() }

        val outputFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { fos ->
            outputBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }
        outputBitmap.recycle()

        Uri.fromFile(outputFile)
    } catch (e: Exception) {
        null
    }
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1024
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1024
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private suspend fun loadOrientedBitmap(context: Context, uri: Uri, maxSize: Int = 2048): Bitmap? =
    withContext(Dispatchers.IO) {
        val uriString = uri.toString()
        android.util.Log.d("AvatarCropDialog", "Loading bitmap for URI: $uriString (scheme=${uri.scheme})")

        // 1. Try Coil ImageLoader first
        // Coil natively handles Contact provider URIs, content://, file://, raw paths, and EXIF orientations
        try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .size(coil.size.Size.ORIGINAL)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bmp = drawableToBitmap(result.drawable)
                val maxDim = maxOf(bmp.width, bmp.height)
                val finalBmp = if (maxDim > maxSize) {
                    val factor = maxSize.toFloat() / maxDim
                    Bitmap.createScaledBitmap(
                        bmp,
                        (bmp.width * factor).toInt().coerceAtLeast(1),
                        (bmp.height * factor).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    bmp
                }
                android.util.Log.d("AvatarCropDialog", "Loaded via Coil: ${finalBmp.width}x${finalBmp.height}")
                return@withContext finalBmp.copy(Bitmap.Config.ARGB_8888, true)
            } else if (result is coil.request.ErrorResult) {
                android.util.Log.w("AvatarCropDialog", "Coil failed for $uriString: ${result.throwable.message}")
            }
        } catch (e: Exception) {
            android.util.Log.w("AvatarCropDialog", "Coil loader threw for $uriString", e)
        }

        // 2. Secondary fallback: Direct stream reader & temp-file decoder
        try {
            val tempFile = File.createTempFile("crop_in_", ".tmp", context.cacheDir)
            try {
                var stream: InputStream? = null

                // Check raw file path or file://
                if (uri.scheme == "file" || uri.scheme == null) {
                    val path = uri.path ?: uriString
                    val file = File(path)
                    if (file.exists()) {
                        stream = file.inputStream()
                    }
                }

                // Check Contacts Provider photo URI
                if (stream == null && uriString.contains("contacts")) {
                    try {
                        stream = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, uri, true)
                    } catch (_: Exception) {}

                    if (stream == null) {
                        try {
                            val baseUriStr = if (uriString.endsWith("/photo") || uriString.endsWith("/display_photo")) {
                                uriString.substringBeforeLast("/")
                            } else uriString
                            stream = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, Uri.parse(baseUriStr), true)
                        } catch (_: Exception) {}
                    }

                    // Query Photo BLOB directly from Data table
                    if (stream == null) {
                        try {
                            val contactId = uri.lastPathSegment?.takeIf { it.toLongOrNull() != null }
                                ?: uri.pathSegments.getOrNull(uri.pathSegments.indexOf("contacts") + 1)?.takeIf { it.toLongOrNull() != null }
                            if (contactId != null) {
                                context.contentResolver.query(
                                    ContactsContract.Data.CONTENT_URI,
                                    arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO),
                                    "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                    arrayOf(contactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE),
                                    null
                                )?.use { cursor ->
                                    if (cursor.moveToFirst()) {
                                        val blob = cursor.getBlob(0)
                                        if (blob != null && blob.isNotEmpty()) {
                                            stream = java.io.ByteArrayInputStream(blob)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }

                // Check openAssetFileDescriptor
                if (stream == null) {
                    try {
                        stream = context.contentResolver.openAssetFileDescriptor(uri, "r")?.createInputStream()
                    } catch (_: Exception) {}
                }

                // Standard openInputStream
                if (stream == null) {
                    try {
                        stream = context.contentResolver.openInputStream(uri)
                    } catch (_: Exception) {}
                }

                val activeStream = stream ?: run {
                    android.util.Log.e("AvatarCropDialog", "Fallback stream was null for $uriString")
                    return@withContext null
                }

                activeStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.length() <= 0L) {
                    android.util.Log.e("AvatarCropDialog", "Temp file empty for $uriString")
                    return@withContext null
                }

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(tempFile.absolutePath, bounds)
                val outW = bounds.outWidth
                val outH = bounds.outHeight
                if (outW <= 0 || outH <= 0) return@withContext null

                var sample = 1
                while (maxOf(outW, outH) / (sample * 2) >= maxSize) {
                    sample *= 2
                }

                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeFile(tempFile.absolutePath, opts) ?: return@withContext null

                val orientation = try {
                    android.media.ExifInterface(tempFile.absolutePath).getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                } catch (_: Exception) {
                    android.media.ExifInterface.ORIENTATION_NORMAL
                }

                val matrix = Matrix()
                when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    android.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    android.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    else -> return@withContext decoded
                }

                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (rotated !== decoded) decoded.recycle()
                android.util.Log.d("AvatarCropDialog", "Loaded via fallback: ${rotated.width}x${rotated.height}")
                rotated
            } finally {
                tempFile.delete()
            }
        } catch (e: Exception) {
            android.util.Log.e("AvatarCropDialog", "All loading strategies failed for $uriString", e)
            null
        }
    }

