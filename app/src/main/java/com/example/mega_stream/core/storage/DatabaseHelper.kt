package com.example.mega_stream.core.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.util.Log

data class Folder(val id: Int, val name: String, val url: String)

class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mega_stream_v30.db" // NEW VERSION to clean all old structures
        private const val DATABASE_VERSION = 1
        private const val TABLE_FOLDERS = "folders"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_URL = "mega_url"
        private const val TABLE_SETTINGS = "settings"
        private const val COLUMN_SETTING_KEY = "setting_key"
        private const val COLUMN_SETTING_VAL = "setting_value"
        private const val TAG = "PIX_DB"

        private const val CREATE_TABLE_FOLDERS = "CREATE TABLE $TABLE_FOLDERS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_NAME TEXT UNIQUE," +
                "$COLUMN_URL TEXT)"

        private const val CREATE_TABLE_SETTINGS = "CREATE TABLE $TABLE_SETTINGS (" +
                "$COLUMN_SETTING_KEY TEXT PRIMARY KEY," +
                "$COLUMN_SETTING_VAL TEXT)"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.i(TAG, "Creating fresh database v30 tables...")
        db.execSQL(CREATE_TABLE_FOLDERS)
        db.execSQL(CREATE_TABLE_SETTINGS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "Upgrading DB from $oldVersion to $newVersion")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOLDERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        onCreate(db)
    }

    fun getAllFolders(): List<Folder> {
        val folders = mutableListOf<Folder>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_FOLDERS ORDER BY $COLUMN_ID ASC", null)
        try {
            if (cursor.moveToFirst()) {
                do {
                    folders.add(Folder(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_URL))
                    ))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            Log.e(TAG, "[ERROR] getAllFolders failed", e)
        } finally {
            cursor.close()
        }
        return folders
    }

    /**
     * ADDITIVE SYNC with Detailed Verification
     */
    fun mergeFolders(newFolders: List<Pair<String, String>>) {
        val db = this.writableDatabase
        Log.i(TAG, "[DB_MERGE_START] Processing ${newFolders.size} potential folders...")
        
        db.beginTransaction()
        try {
            val values = ContentValues()
            var inserted = 0
            var updated = 0
            
            for (folder in newFolders) {
                values.clear()
                values.put(COLUMN_NAME, folder.first)
                values.put(COLUMN_URL, folder.second)
                
                // Using REPLACE: If Name exists, it updates URL. If not, it inserts.
                val id = db.insertWithOnConflict(TABLE_FOLDERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                
                if (id != -1L) {
                    Log.d(TAG, "[DB_SUCCESS] Folder '${folder.first}' -> ID: $id")
                    inserted++
                } else {
                    Log.e(TAG, "[DB_FAILURE] Folder '${folder.first}' could not be inserted.")
                }
            }
            db.setTransactionSuccessful()
            Log.i(TAG, "[DB_MERGE_END] Done. Inserted/Updated: $inserted folders.")
        } catch (e: Exception) {
            Log.e(TAG, "[CRITICAL] Transaction failed: ${e.message}", e)
        } finally {
            db.endTransaction()
        }
    }

    fun saveSetting(key: String, value: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SETTING_KEY, key)
            put(COLUMN_SETTING_VAL, value)
        }
        db.replace(TABLE_SETTINGS, null, values)
        Log.i(TAG, "[SETTING_SAVE] $key = $value")
    }

    fun getSetting(key: String, defaultValue: String): String {
        val db = this.readableDatabase
        var result = defaultValue
        val cursor = db.query(
            TABLE_SETTINGS, arrayOf(COLUMN_SETTING_VAL),
            "$COLUMN_SETTING_KEY = ?", arrayOf(key),
            null, null, null
        )
        try {
            if (cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SETTING_VAL))
            }
        } finally {
            cursor.close()
        }
        return result
    }

    fun isFirstLaunch(): Boolean {
        return getSetting("setup_complete", "false") == "false"
    }

    fun completeSetup() {
        saveSetting("setup_complete", "true")
    }

    fun resetAllData() {
        val db = this.writableDatabase
        db.delete(TABLE_FOLDERS, null, null)
        db.delete(TABLE_SETTINGS, null, null)
        Log.i(TAG, "[DB_RESET] Database wiped clean.")
    }
}
