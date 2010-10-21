package nikeno.Tenki;

import java.io.ByteArrayOutputStream;
import java.util.WeakHashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

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
	static public byte[] download(String url, int maxSize) throws Exception {
		//Log.d(TAG, "downloading " + url);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(20*1024);
		
		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet	request = new HttpGet(url);
		
		HttpResponse response = client.execute(request);

		response.getEntity().writeTo(baos);
			
		return baos.toByteArray();
	}
	
	static public byte[] download(String url, int maxSize, long since, boolean storeCache) throws Exception {
		byte[] data = null;
		if (since != -1) {
			data = mFileCache.get(url, since);
			if (data != null) {
				return data;
			}
		}
		data = download(url, maxSize);
		if (data != null && storeCache) {
			mFileCache.put(url, data);
		}
		
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

}
