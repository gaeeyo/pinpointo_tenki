package nikeno.Tenki;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.WeakHashMap;

import javax.net.ssl.HttpsURLConnection;

public class Downloader {
    static final  String DEFAULT_CACHE_FILENAME = "file_cache.db";
    static Downloader sInstance;
    private final String TAG                    = "Downloader";
    private final FileCache mFileCache;
    private final WeakHashMap<String, Bitmap> mBitmapCache;

    public Downloader(Context context, String filename) {
        mFileCache = new FileCache(context, filename);
        mBitmapCache = new WeakHashMap<>();
    }

    public static synchronized Downloader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new Downloader(context.getApplicationContext(),
                    DEFAULT_CACHE_FILENAME);
        }
        return sInstance;
    }

    public byte[] download(String url, int maxSize, boolean storeCache) throws Exception {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "downloading " + url);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream(maxSize);

        URL u = new URL(url);

        URLConnection uc = u.openConnection();
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

    public byte[] download(String url, int maxSize, long since, boolean storeCache) throws Exception {
        byte[] data = null;
        if (since != -1) {
            FileCache.Entry entry = mFileCache.get(url, since);
            if (entry != null) {
                return entry.data;
            }
        }
        data = download(url, maxSize, storeCache);

        return data;
    }

    public FileCache.Entry getCache(String url, long since) {
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

    public Bitmap getImageFromMemCache(String url) {
        return mBitmapCache.get(url);
    }

    public interface ImageHandler {
        void setBitmap(Bitmap bmp);
    }

    public static class ImageViewSetter implements ImageHandler {
        ImageView mView;

        public ImageViewSetter(ImageView iv) {
            mView = iv;
        }

        @Override
        public void setBitmap(Bitmap bmp) {
            mView.setImageBitmap(bmp);
        }
    }
}
