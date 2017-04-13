package nikeno.Tenki;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileCache {
    static final String           TAG          = "FileCache";
    static final int              DB_VERSION   = 3;
    static final SimpleDateFormat LOG_TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private SQLiteDatabase mDB;

    public FileCache(Context c, String filename) {
        DBHelper db = new DBHelper(c, filename);
        mDB = db.getWritableDatabase();
    }

    public void put(String key, byte[] data, long lastModified) {
        Log.d(TAG, "put " + key + " lastModified:" + LOG_TIME_FMT.format(new Date(lastModified)));
        ContentValues values = new ContentValues();
        values.put(CacheTable.KEY, key);
        values.put(CacheTable.VALUE, data);
        values.put(CacheTable.TIME, System.currentTimeMillis());
        values.put(CacheTable.LAST_MODIFIED, lastModified);
        mDB.replace(CacheTable.TABLE_NAME, null, values);
    }

    public Entry get(String key, long since) {
        Cursor cur   = null;
        Entry  entry = null;
        try {
            if (since != 0) {
                cur = mDB.query(CacheTable.TABLE_NAME,
                        new String[]{CacheTable.VALUE, CacheTable.TIME},
                        CacheTable.KEY + "=?"
                                + " AND " + CacheTable.TIME + ">?",
                        new String[]{
                                key, String.valueOf(since)
                        },
                        null, null, null, "1");
            } else {
                cur = mDB.query(CacheTable.TABLE_NAME,
                        new String[]{CacheTable.VALUE, CacheTable.TIME},
                        CacheTable.KEY + "=?",
                        new String[]{key},
                        null, null, null, "1");
            }
            if (cur.moveToFirst()) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "get Found (" + key + ")");
                }
                entry = new Entry();
                entry.data = cur.getBlob(0);
                entry.time = cur.getLong(1);
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "get NotFound (" + key + ")");
                }
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return entry;
    }

    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context c, String filename) {
            super(c, filename, null, DB_VERSION);
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

        public static final String KEY           = "key";
        public static final String TIME          = "time";
        public static final String VALUE         = "value";
        public static final String LAST_MODIFIED = "lastModified";

        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_NAME + "("
                        + KEY + " TEXT PRIMARY KEY,"
                        + TIME + " LONG NOT NULL,"
                        + VALUE + " BLOB NOT NULL,"
                        + LAST_MODIFIED + " LONG NOT NULL DEFAULT 0"
                        + ")";
    }

    public static class Entry {
        public long   time;
        public byte[] data;

    }
}
