package nikeno.Tenki;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.WeakHashMap;

public class Downloader {
	@SuppressWarnings("unused")
	static private final String TAG = "Downloader";

	static private FileCache mFileCache = null;
	static private WeakHashMap<String, Bitmap> mBitmapCache = null;

	// 初期化
	static public void initialize(Context c) {
		if (mFileCache == null) {
			mFileCache = new FileCache(c);
		}
		if (mBitmapCache == null) {
			mBitmapCache = new WeakHashMap<String, Bitmap>();
		}
	}

	// ダウンロード
	// since == -1 : キャッシュから読み込まない
	static public byte[] download(String url, int maxSize, boolean storeCache) throws Exception {
		if (Const.DEBUG) {
			Log.d(TAG, "downloading " + url);
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream(maxSize);

		URL u = new URL(url);

		URLConnection uc = u.openConnection();
		uc.setConnectTimeout(Const.CONNECT_TIMEOUT);
		uc.getIfModifiedSince();

		InputStream in = uc.getInputStream();
		try {
			byte [] buf = new byte [8*1024];
			int readSize;
			while ((readSize = in.read(buf)) > 0) {
				baos.write(buf, 0, readSize);
			}
		}
		finally {
			in.close();
		}

		byte [] data = baos.toByteArray();
		if (data != null && storeCache) {
			mFileCache.put(url, data, uc.getIfModifiedSince());
		}

		return data;
	}

	static public byte[] download(String url, int maxSize, long since, boolean storeCache) throws Exception {
		byte[] data = null;
		if (since != -1) {
			data = mFileCache.get(url, since);
			if (data != null) {
				return data;
			}
		}
		data = download(url, maxSize, storeCache);

		return data;
	}

	static public byte[] getCache(String url, long since) {
		return mFileCache.get(url, since);
	}

//	static public String downloadHtml(String url, String encoding,
//			int maxSize, long since, boolean storeCache) throws Exception {
//
//		byte [] htmlData = mFileCache.get(url, since);
//		if (htmlData != null) {
//			return new String(htmlData);
//		}
//
//		String html = null;
//
//		htmlData = download(url, maxSize);
//		if (htmlData != null) {
//			html = new String(htmlData, encoding);
//
//			if (storeCache) {
//				mFileCache.put(url, html.getBytes());
//			}
//		}
//		return html;
//	}

	static public Bitmap downloadImage(String url, int maxSize, long since) throws Exception {
		Bitmap bmp = mBitmapCache.get(url);
		if (bmp == null) {
			byte[] data = download(url, maxSize, since, true);
			if (data != null) {
				bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
				bmp.setDensity(DisplayMetrics.DENSITY_MEDIUM);
				mBitmapCache.put(url, bmp);
			}
		}
		else {
			//Log.d(TAG, "Found in BitmapCache");
		}
		return bmp;
	}

	static public Bitmap getImageFromMemCache(String url)  {
		return mBitmapCache.get(url);
	}

}
