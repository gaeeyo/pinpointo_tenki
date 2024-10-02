package nikeno.Tenki;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.WeakHashMap;

import javax.net.ssl.HttpsURLConnection;

import nikeno.Tenki.db.ResourceCache;
import nikeno.Tenki.db.entity.ResourceCacheEntity;

public class Downloader {
    private final        String                      TAG                    = "Downloader";
    private final        ResourceCache               mFileCache;
    private final        WeakHashMap<String, Bitmap> mBitmapCache;

    public Downloader(Context context, String filename) {
        mFileCache = new ResourceCache(context, filename);
        mBitmapCache = new WeakHashMap<>();
    }

    public byte[] download(String url, int maxSize, boolean storeCache) throws Exception {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "downloading " + url);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(maxSize);

        URL u = new URL(url);

        HttpURLConnection uc = (HttpURLConnection) u.openConnection();
        uc.setRequestProperty("User-Agent", "pinpoint_tenki/" + BuildConfig.VERSION_NAME);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT && uc instanceof HttpsURLConnection) {
            // Android 4.4未満でTLS1.2を有効化する
            ((HttpsURLConnection) uc).setSSLSocketFactory(new TLSSocketFactory());
        }
        uc.setConnectTimeout(TenkiApp.CONNECT_TIMEOUT);
        uc.getIfModifiedSince();

        InputStream in = uc.getInputStream();
        try {
            byte[] buf = new byte[8 * 1024];
            int    readSize;
            while ((readSize = in.read(buf)) > 0) {
                baos.write(buf, 0, readSize);
            }
        } finally {
            in.close();
        }

        byte[] data = baos.toByteArray();
        if (data != null && storeCache) {
            mFileCache.put(url, data, uc.getIfModifiedSince());
        }

        return data;
    }

    static int sFakeError = BuildConfig.DEBUG ? 0 : 0;

    public byte[] download(String url, int maxSize, long since, boolean storeCache) throws Exception {
        if (sFakeError > 0 && url.endsWith(".html")) {
            sFakeError--;
            Log.w(TAG, "Fake Error");
            throw new IOException("Fake Error");
        }
        if (since != -1) {
            ResourceCacheEntity entry = mFileCache.get(url, since);
            if (entry != null) {
                return entry.data;
            }
        }
        return download(url, maxSize, storeCache);
    }

    public ResourceCacheEntity getCache(String url, long since) {
        return mFileCache.get(url, since);
    }

    public Bitmap downloadImage(String url, int maxSize, long since) throws Exception {
        Bitmap bmp = mBitmapCache.get(url);
        if (bmp == null) {
            byte[] data = download(url, maxSize, since, true);
            if (data != null) {
                bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                bmp.setDensity(DisplayMetrics.DENSITY_MEDIUM);
                mBitmapCache.put(url, bmp);
            }
        } else {
            //Log.d(TAG, "Found in BitmapCache");
        }
        return bmp;
    }

    Bitmap getImageFromMemCache(String url) {
        return mBitmapCache.get(url);
    }

    public interface ImageHandler {
        void setBitmap(Bitmap bmp);
    }
}
