package nikeno.Tenki

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.DisplayMetrics
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.lastModified
import kotlinx.datetime.Clock
import nikeno.Tenki.db.ResourceCache
import nikeno.Tenki.db.entity.ResourceCacheEntity
import java.util.WeakHashMap

class Downloader(context: Context?, filename: String?, private val client: HttpClient) {
    private val TAG = "Downloader"
    private val mFileCache = ResourceCache(context, filename)
    private val mBitmapCache = WeakHashMap<String, Bitmap?>()

    suspend fun download(url: String, storeCache: Boolean): ByteArray {
        val res = client.get(url)
        val body = res.body<ByteArray>()
        if (storeCache) {
            mFileCache.put(
                url,
                body,
                res.lastModified()?.time ?: Clock.System.now().toEpochMilliseconds()
            )
        }
        return body
    }

    suspend fun download(url: String, since: Long, storeCache: Boolean): ByteArray {
        if (since != -1L) {
            val entry = mFileCache[url, since]
            if (entry != null) {
                return entry.data
            }
        }
        return download(url, storeCache)
    }

    fun getCache(url: String?, since: Long): ResourceCacheEntity? {
        return mFileCache[url, since]
    }

    @Throws(Exception::class)
    suspend fun downloadImage(url: String, since: Long): Bitmap {
        val data = download(url, since, true)
        val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
        bmp.setDensity(DisplayMetrics.DENSITY_MEDIUM)
        return bmp
    }
}
