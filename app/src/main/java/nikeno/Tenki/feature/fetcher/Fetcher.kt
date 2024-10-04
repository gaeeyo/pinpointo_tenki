package nikeno.Tenki.feature.fetcher

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nikeno.Tenki.Downloader
import java.util.concurrent.ConcurrentHashMap

class ImageFetcher(val downloader: Downloader) {

    private val mutex = Mutex()
    private val bitmapCache = ConcurrentHashMap<String, Bitmap?>()

    suspend fun getImage2(url: String): Bitmap? {
        bitmapCache.get(url)?.let { return it }

        mutex.withLock {
            bitmapCache[url]?.let { return it }
            try {
                val bmp = downloadImage(url)
                bitmapCache[url] = bmp
                return bmp
            } catch (e: Exception) {
                bitmapCache[url] = null
                return null
            }
        }
    }

    private suspend fun downloadImage(url: String): Bitmap {
        val data = downloader.download(url, 0, true)
        val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
        bmp?.setDensity(DisplayMetrics.DENSITY_MEDIUM)
        return bmp
    }
}
