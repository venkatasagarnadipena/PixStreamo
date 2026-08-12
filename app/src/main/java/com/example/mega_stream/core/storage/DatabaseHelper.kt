package com.example.mega_stream.core.storage

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.util.Log

data class Folder(val id: Int, val name: String, val url: String)

class DatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.enableWriteAheadLogging()
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("DatabaseHelper", "Creating tables...")
        db.execSQL(CREATE_TABLE_FOLDERS)
        db.execSQL(CREATE_TABLE_SETTINGS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DatabaseHelper", "Upgrading DB from $oldVersion to $newVersion")
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
            Log.e("DatabaseHelper", "Error reading folders", e)
        } finally {
            cursor.close()
        }
        Log.d("DatabaseHelper", "Retrieved ${folders.size} folders from DB")
        return folders
    }

    fun mergeFolders(newFolders: List<Pair<String, String>>) {
        val db = this.writableDatabase
        Log.d("DatabaseHelper", "Starting merge of ${newFolders.size} folders")
        db.beginTransaction()
        try {
            // Option 1: Strictly match the remote list (Clear and Refill)
            // This is safer if you want the TV to EXACTLY match your latest JSON file.
            db.delete(TABLE_FOLDERS, null, null)
            Log.d("DatabaseHelper", "Existing folders cleared for fresh sync.")

            val values = ContentValues()
            for (folder in newFolders) {
                values.clear()
                values.put(COLUMN_NAME, folder.first)
                values.put(COLUMN_URL, folder.second)
                val id = db.insert(TABLE_FOLDERS, null, values)
                Log.d("DatabaseHelper", "Inserted folder ${folder.first} with ID $id")
            }
            db.setTransactionSuccessful()
            Log.d("DatabaseHelper", "Transaction successful. Merge complete.")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Merge failed", e)
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
        val affected = db.replace(TABLE_SETTINGS, null, values)
        Log.d("DatabaseHelper", "Saved setting $key=$value (rows affected: $affected)")
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
        Log.d("DatabaseHelper", "All data reset.")
    }

    companion object {
        private const val DATABASE_NAME = "mega_stream_v21.db" // Incremented to v21 to force refresh
        private const val DATABASE_VERSION = 1
        private const val TABLE_FOLDERS = "folders"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_URL = "mega_url"
        private const val TABLE_SETTINGS = "settings"
        private const val COLUMN_SETTING_KEY = "setting_key"
        private const val COLUMN_SETTING_VAL = "setting_value"

        private const val CREATE_TABLE_FOLDERS = "CREATE TABLE $TABLE_FOLDERS (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COLUMN_NAME TEXT," +
                "$COLUMN_URL TEXT," +
                "UNIQUE($COLUMN_NAME, $COLUMN_URL))"

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
}
