package com.example.ui.viewers

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.CategoryAudio
import com.example.util.FileUtils
import com.example.util.MediaThumbnailLoader
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun AudioPlayerDialog(
    file: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var isLooping by remember { mutableStateOf(false) }

    var coverArt by remember(file.absolutePath) {
        mutableStateOf(MediaThumbnailLoader.getCachedThumbnail(file.absolutePath))
    }

    LaunchedEffect(file.absolutePath) {
        if (coverArt == null) {
            coverArt = MediaThumbnailLoader.loadAudioCoverArt(file)
        }
    }

    val mediaPlayer = remember {
        MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                duration = this.duration
                start()
                isPlaying = true
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            try {
                if (mediaPlayer.isPlaying) {
                    currentPosition = mediaPlayer.currentPosition
                }
            } catch (_: Exception) {}
            delay(350)
        }
    }

    DisposableEffect(Unit) {
        mediaPlayer.setOnCompletionListener {
            if (!isLooping) {
                isPlaying = false
                currentPosition = 0
            }
        }
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(28.dp))
                .testTag("audio_player_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = stringResource(R.string.audio_player),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = { FileUtils.shareFiles(context, listOf(file)) }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Music Art / Cover Art
                if (coverArt != null) {
                    Image(
                        bitmap = coverArt!!.asImageBitmap(),
                        contentDescription = "Album art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(130.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(CategoryAudio.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = CategoryAudio,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Song / Audio Info
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = FileUtils.formatFileSize(file.length()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Timeline Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { newPos ->
                            currentPosition = newPos.toInt()
                            try {
                                mediaPlayer.seekTo(newPos.toInt())
                            } catch (_: Exception) {}
                        },
                        valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isLooping = !isLooping
                            mediaPlayer.isLooping = isLooping
                        }
                    ) {
                        Icon(
                            imageVector = if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            try {
                                val newPos = (mediaPlayer.currentPosition - 10000).coerceAtLeast(0)
                                mediaPlayer.seekTo(newPos)
                                currentPosition = newPos
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                try {
                                    if (mediaPlayer.isPlaying) {
                                        mediaPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        mediaPlayer.start()
                                        isPlaying = true
                                    }
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            try {
                                val newPos = (mediaPlayer.currentPosition + 10000).coerceAtMost(duration)
                                mediaPlayer.seekTo(newPos)
                                currentPosition = newPos
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

private fun formatTime(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
