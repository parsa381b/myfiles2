package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaThumbnailLoader {

    // In-memory cache for fast UI recycling
    private val memoryCache = object : LruCache<String, Bitmap>(120) {}
    private val noThumbnailCache = HashSet<String>()

    fun getCachedThumbnail(path: String): Bitmap? {
        synchronized(memoryCache) {
            return memoryCache.get(path)
        }
    }

    suspend fun loadAudioCoverArt(file: File): Bitmap? = withContext(Dispatchers.IO) {
        val path = file.absolutePath
        synchronized(noThumbnailCache) {
            if (noThumbnailCache.contains(path)) return@withContext null
        }
        synchronized(memoryCache) {
            val cached = memoryCache.get(path)
            if (cached != null) return@withContext cached
        }

        val mmr = MediaMetadataRetriever()
        try {
            mmr.setDataSource(path)
            val picture = mmr.embeddedPicture
            if (picture != null) {
                // Decode with inSampleSize to conserve heap memory
                val boundsOnly = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(picture, 0, picture.size, boundsOnly)

                var sampleSize = 1
                while (boundsOnly.outWidth / sampleSize > 160 || boundsOnly.outHeight / sampleSize > 160) {
                    sampleSize *= 2
                }

                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size, options)
                if (bitmap != null) {
                    synchronized(memoryCache) {
                        memoryCache.put(path, bitmap)
                    }
                    return@withContext bitmap
                }
            }
            synchronized(noThumbnailCache) {
                noThumbnailCache.add(path)
            }
            null
        } catch (_: Exception) {
            synchronized(noThumbnailCache) {
                noThumbnailCache.add(path)
            }
            null
        } finally {
            try {
                mmr.release()
            } catch (_: Exception) {}
        }
    }

    suspend fun loadVideoThumbnail(file: File): Bitmap? = withContext(Dispatchers.IO) {
        val path = file.absolutePath
        synchronized(noThumbnailCache) {
            if (noThumbnailCache.contains(path)) return@withContext null
        }
        synchronized(memoryCache) {
            val cached = memoryCache.get(path)
            if (cached != null) return@withContext cached
        }

        try {
            var bitmap: Bitmap? = null

            // 1. Android Q+ ThumbnailUtils
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    bitmap = ThumbnailUtils.createVideoThumbnail(file, Size(200, 200), null)
                } catch (_: Throwable) {}
            }

            // 2. MediaMetadataRetriever
            if (bitmap == null) {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(path)
                    bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        mmr.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 200, 200)
                            ?: mmr.frameAtTime
                    } else {
                        mmr.frameAtTime
                    }
                } catch (_: Throwable) {
                } finally {
                    try { mmr.release() } catch (_: Exception) {}
                }
            }

            // 3. Fallback to legacy ThumbnailUtils
            if (bitmap == null) {
                @Suppress("DEPRECATION")
                try {
                    bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND)
                } catch (_: Throwable) {}
            }

            if (bitmap != null) {
                synchronized(memoryCache) {
                    memoryCache.put(path, bitmap)
                }
                return@withContext bitmap
            }

            synchronized(noThumbnailCache) {
                noThumbnailCache.add(path)
            }
            null
        } catch (_: Throwable) {
            synchronized(noThumbnailCache) {
                noThumbnailCache.add(path)
            }
            null
        }
    }

    suspend fun loadApkIcon(context: Context, file: File): Bitmap? = withContext(Dispatchers.IO) {
        val path = file.absolutePath
        synchronized(noThumbnailCache) {
            if (noThumbnailCache.contains(path)) return@withContext null
        }
        synchronized(memoryCache) {
            val cached = memoryCache.get(path)
            if (cached != null) return@withContext cached
        }

        try {
            val ext = file.extension.lowercase()
            if (ext == "xapk" || ext == "apks") {
                val details = PackageInstallerHelper.parsePackageDetails(context, file)
                val bmp = details?.icon
                if (bmp != null) {
                    synchronized(memoryCache) {
                        memoryCache.put(path, bmp)
                    }
                    return@withContext bmp
                }
            }

            val pm = context.packageManager
            val packageInfo = pm.getPackageArchiveInfo(path, 0)
            val appInfo = packageInfo?.applicationInfo
            if (appInfo != null) {
                appInfo.sourceDir = path
                appInfo.publicSourceDir = path
                val drawable = appInfo.loadIcon(pm)
                val bitmap = drawableToBitmap(drawable)
                if (bitmap != null) {
                    synchronized(memoryCache) {
                        memoryCache.put(path, bitmap)
                    }
                    return@withContext bitmap
                }
            }
            synchronized(noThumbnailCache) {
                noThumbnailCache.add(path)
            }
            null
        } catch (_: Exception) {
            synchronized(noThumbnailCache) {
                noThumbnailCache.add(path)
            }
            null
        }
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceAtMost(160) else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceAtMost(160) else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
