package nikeno.Tenki;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.format.DateUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FileCache {
	static final String 	TAG = "FileCache";
	static final int 		DB_VERSION = 3;
	static final String 	DB_NAME = "file_cache.db";
	static final SimpleDateFormat LOG_TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private SQLiteDatabase mDB;

	public FileCache(Context c) {
		DBHelper db = new DBHelper(c);
		mDB = db.getWritableDatabase();
	}

	public void expire() {

		long expire = System.currentTimeMillis() - 30 * DateUtils.DAY_IN_MILLIS;

		mDB.delete(CacheTable.TABLE_NAME, "time < ?",
				new String[] { String.valueOf(expire) });
	}

	// データを保存
	public void put(String key, byte[] data, long lastModified) {
		Log.d(TAG, "put " + key + " lastModified:" + LOG_TIME_FMT.format(new Date(lastModified)));
		ContentValues values = new ContentValues();
		values.put(CacheTable.KEY, key);
		values.put(CacheTable.VALUE, data);
		values.put(CacheTable.TIME, System.currentTimeMillis());
		values.put(CacheTable.LAST_MODIFIED, lastModified);
		mDB.replace(CacheTable.TABLE_NAME, null, values);
	}

	// データを取得
	public byte[] get(String key, long modifiedSince) {
		Cursor cur = null;
		byte[] data = null;
		try {
			if (modifiedSince != 0) {
				cur = mDB.query(CacheTable.TABLE_NAME,
						new String[] { CacheTable.VALUE },
						CacheTable.KEY + "=?"
						+ " AND " + CacheTable.TIME + ">?",
						new String[] {
							key, String.valueOf(modifiedSince)
						},
						null, null, null, "1");
			} else {
				cur = mDB.query(CacheTable.TABLE_NAME,
						new String[] { CacheTable.VALUE },
						CacheTable.KEY + "=?",
						new String[] { key },
						null, null, null, "1");
			}
			if (cur.moveToFirst()) {
				if (Const.DEBUG) {
					Log.d(TAG, "get Found ("+key+")");
				}
				data = cur.getBlob(0);
			} else {
				if (Const.DEBUG) {
					Log.d(TAG, "get NotFound ("+key+")");
				}
			}
		} finally {
			if (cur != null) {
				cur.close();
			}
		}
		return data;
	}

	private static class DBHelper extends SQLiteOpenHelper {

		static boolean ALWAYS_CLEAN = false;

		public DBHelper(Context c) {
			super(c, DB_NAME, null, DB_VERSION);
		}
		@Override
		public synchronized SQLiteDatabase getWritableDatabase() {
			SQLiteDatabase db = super.getWritableDatabase();

			if (ALWAYS_CLEAN) {
				onUpgrade(db, 0, 0);
				ALWAYS_CLEAN = false;
			}

			return db;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CacheTable.CREATE_TABLE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + CacheTable.TABLE_NAME);
			onCreate(db);
		}
	}

	public static class CacheTable {
		public static final String TABLE_NAME = "file_cache";

		public static final String KEY = "key";
		public static final String TIME = "time";
		public static final String VALUE = "value";
		public static final String LAST_MODIFIED = "lastModified";

		public static final String CREATE_TABLE =
				"CREATE TABLE IF NOT EXISTS "
				+ TABLE_NAME + "("
				+ KEY + " TEXT PRIMARY KEY,"
				+ TIME + " DATETIME NOT NULL,"
				+ VALUE + " BLOB NOT NULL,"
				+ LAST_MODIFIED + " LONG NOT NULL DEFAULT 0"
				+ ")";
	}

}
