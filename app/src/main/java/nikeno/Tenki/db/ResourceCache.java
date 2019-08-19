package nikeno.Tenki.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nikeno.Tenki.BuildConfig;
import nikeno.Tenki.db.entity.ResourceCacheEntity;

public class ResourceCache {
    private static final String           TAG          = "ResourceCache";
    private static final int              DB_VERSION   = 3;
    private static final SimpleDateFormat LOG_TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

    private SQLiteDatabase mDB;

    public ResourceCache(Context c, String filename) {
        DBHelper db = new DBHelper(c, filename);
        mDB = db.getWritableDatabase();
    }

    public void put(String key, byte[] data, long lastModified) {
        Log.d(TAG, "put " + key + " lastModified:" + LOG_TIME_FMT.format(new Date(lastModified)));
        ContentValues values = new ContentValues();
        values.put(ResourceCacheTable.KEY, key);
        values.put(ResourceCacheTable.VALUE, data);
        values.put(ResourceCacheTable.TIME, System.currentTimeMillis());
        values.put(ResourceCacheTable.LAST_MODIFIED, lastModified);
        mDB.replace(ResourceCacheTable.TABLE_NAME, null, values);
    }

    public ResourceCacheEntity get(String key, long since) {
        Cursor              cur   = null;
        ResourceCacheEntity entry = null;
        try {
            if (since != 0) {
                cur = mDB.query(ResourceCacheTable.TABLE_NAME,
                        new String[]{ResourceCacheTable.VALUE, ResourceCacheTable.TIME},
                        ResourceCacheTable.KEY + "=?"
                                + " AND " + ResourceCacheTable.TIME + ">?",
                        new String[]{
                                key, String.valueOf(since)
                        },
                        null, null, null, "1");
            } else {
                cur = mDB.query(ResourceCacheTable.TABLE_NAME,
                        new String[]{ResourceCacheTable.VALUE, ResourceCacheTable.TIME},
                        ResourceCacheTable.KEY + "=?",
                        new String[]{key},
                        null, null, null, "1");
            }
            if (cur.moveToFirst()) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "get Found (" + key + ")");
                }
                entry = new ResourceCacheEntity();
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

        DBHelper(Context c, String filename) {
            super(c, filename, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(ResourceCacheTable.CREATE_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + ResourceCacheTable.TABLE_NAME);
            onCreate(db);
        }
    }


    private static class ResourceCacheTable {
        static final String TABLE_NAME = "file_cache";

        static final String KEY           = "`key`";
        static final String TIME          = "time";
        static final String VALUE         = "value";
        static final String LAST_MODIFIED = "lastModified";

        static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS "
                        + TABLE_NAME + "("
                        + KEY + " TEXT PRIMARY KEY,"
                        + TIME + " LONG NOT NULL,"
                        + VALUE + " BLOB NOT NULL,"
                        + LAST_MODIFIED + " LONG NOT NULL DEFAULT 0"
                        + ")";
    }
}
