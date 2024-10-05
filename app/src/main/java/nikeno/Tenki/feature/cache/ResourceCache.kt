package nikeno.Tenki.feature.cache

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import nikeno.Tenki.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ResourceCache"

class ResourceCache(context: Context, filename: String) {

    private val db: SQLiteDatabase = DBHelper(context, filename).writableDatabase

    fun delete(key: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "delete $key")
        }
        db.delete(
            ResourceCacheTable.TABLE_NAME,
            "${Columns.KEY}=?",
            arrayOf(key)
        )
    }

    fun put(key: String, data: ByteArray?, lastModified: Long) {
        Log.d(
            TAG,
            "put key=$key, size=${data?.size}, lastModified=" + LOG_TIME_FMT.format(
                Date(lastModified)
            )
        )
        val values = ContentValues()
        values.put(Columns.KEY, key)
        values.put(Columns.VALUE, data)
        values.put(Columns.TIME, System.currentTimeMillis())
        values.put(Columns.LAST_MODIFIED, lastModified)
        db.replace(ResourceCacheTable.TABLE_NAME, null, values)
    }

    fun get(key: String, since: Long): ResourceCacheEntity? {

        if (since != 0L) {
            db.query(
                ResourceCacheTable.TABLE_NAME,
                arrayOf(Columns.VALUE, Columns.TIME),
                Columns.KEY + "=?"
                        + " AND " + Columns.TIME + ">?",
                arrayOf(
                    key, since.toString()
                ),
                null, null, null, "1"
            )
        } else {
            db.query(
                ResourceCacheTable.TABLE_NAME,
                arrayOf(Columns.VALUE, Columns.TIME),
                Columns.KEY + "=?",
                arrayOf(key),
                null, null, null, "1"
            )
        }.use { cur ->
            if (cur.moveToFirst()) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "get($key) hit")
                }
                return ResourceCacheEntity(
                    cur.getLong(1),
                    cur.getBlob(0),
                )
            } else {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "get($key) miss")
                }
                return null
            }
        }
    }


    private class DBHelper(c: Context, filename: String) :
        SQLiteOpenHelper(c, filename, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(ResourceCacheTable.CREATE_TABLE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS " + ResourceCacheTable.TABLE_NAME)
            onCreate(db)
        }
    }

    private object Columns {
        const val KEY: String = "`key`"
        const val TIME: String = "time"
        const val VALUE: String = "value"
        const val LAST_MODIFIED: String = "lastModified"
    }

    private object ResourceCacheTable {
        const val TABLE_NAME: String = "file_cache"


        const val CREATE_TABLE: String = ("CREATE TABLE IF NOT EXISTS "
                + TABLE_NAME + "("
                + Columns.KEY + " TEXT PRIMARY KEY,"
                + Columns.TIME + " LONG NOT NULL,"
                + Columns.VALUE + " BLOB NOT NULL,"
                + Columns.LAST_MODIFIED + " LONG NOT NULL DEFAULT 0"
                + ")")
    }

    companion object {
        private const val TAG = "ResourceCache"
        private const val DB_VERSION = 3
        private val LOG_TIME_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
    }
}
