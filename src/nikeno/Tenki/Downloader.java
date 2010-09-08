package nikeno.Tenki;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

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
		byte [] data;
		
		InputStream is = null;
		BufferedInputStream bis = null;
		try {
			URL aURL = new URL(url);
			URLConnection conn = aURL.openConnection();
			conn.connect();
			is = conn.getInputStream();
			bis = new BufferedInputStream(is);

			byte[] buff = new byte[maxSize];
			
			int dataSize = 0;
			int readSize = 0;
			while ((readSize = bis.read(buff, dataSize, maxSize - dataSize)) >= 0) {
				dataSize += readSize;
			}
			
			data = new byte[dataSize];
			System.arraycopy(buff, 0, data, 0, dataSize);
			
		} finally {
			if (bis != null)
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
				mBitmapCache.put(url, bmp);
			}
		}
		else {
			//Log.d(TAG, "Found in BitmapCache");
		}
		return bmp;
	}

}
