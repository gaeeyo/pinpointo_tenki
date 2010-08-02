package nikeno.Tenki;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;



public class FileCache  {
	static final String TAG = "FileCache";
	static final int DB_VERSION = 1;
	static final String DB = "file_cache.db";
	static final String DB_TABLE = "file_cache";
	
	private SQLiteDatabase mDB; 

	public FileCache(Context c)
	{
		DBHelper db = new DBHelper(c);
		mDB = db.getWritableDatabase();
	}
	
	// データを保存
	public void put(String key, byte [] data)
	{
		Log.d(TAG, "put "+key);
		ContentValues values = new ContentValues();
		values.put("key", key);
		values.put("value", data);
		values.put("time", System.currentTimeMillis()/1000);
		mDB.replace(DB_TABLE, null, values);
	}
	
	// データを取得
	public byte [] get(String key, long modifiedSince)
	{
		Cursor cursor = null;
		byte [] data = null;
		try {
			if (modifiedSince != 0) {
				cursor = mDB.query(DB_TABLE, 
						new String [] { "value" },
						"key = ? AND time > ?", 
						new String [] { key, String.format("%d", modifiedSince) }, 
						null, null, null, "1");
			}
			else {
				cursor = mDB.query(DB_TABLE, 
						new String [] { "value" },
						"key = ?", 
						new String [] { key }, 
						null, null, null, "1");
			}
			if (cursor.moveToFirst()) {
				//Log.d(TAG, "get Found ("+key+")");
				data = cursor.getBlob(0);
			}
			else {
				//Log.d(TAG, "get NotFound ("+key+")");
			}
		}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return data;
	}
	

	private class DBHelper extends SQLiteOpenHelper {
		
		public DBHelper(Context c) {
			super(c, DB, null, DB_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(
					"CREATE TABLE IF NOT EXISTS "
					+DB_TABLE 
					+"("
					+" key TEXT PRIMARY KEY,"
					+" time DATETIME NOT NULL,"
					+" value BLOB NOT NULL"
					+")");
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS "+DB_TABLE);
			onCreate(db);
		}
	}
	
	
}
