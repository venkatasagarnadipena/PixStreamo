package com.example.mega_stream.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.util.Log

data class Folder(val id: Int, val name: String, val url: String)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_FOLDERS)
        db.execSQL(CREATE_TABLE_SETTINGS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_SETTINGS)
        }
        if (oldVersion < 3) {
            // Version 3 adds support for session timestamps
        }
    }

    fun updateLastActiveTime() {
        saveSetting("last_active_time", System.currentTimeMillis().toString())
    }

    fun getLastActiveTime(): Long {
        return getSetting("last_active_time", "0").toLong()
    }

    fun mergeFolders(newFolders: List<Pair<String, String>>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            for (folder in newFolders) {
                val folderName = folder.first
                val folderUrl = folder.second

                val cursor = db.rawQuery(
                    "SELECT 1 FROM $TABLE_FOLDERS WHERE $COLUMN_NAME = ? AND $COLUMN_URL = ?",
                    arrayOf(folderName, folderUrl)
                )
                
                val exists = cursor.moveToFirst()
                cursor.close()

                if (!exists) {
                    val values = ContentValues().apply {
                        put(COLUMN_NAME, folderName)
                        put(COLUMN_URL, folderUrl)
                    }
                    db.insert(TABLE_FOLDERS, null, values)
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error merging folders", e)
        } finally {
            try {
                db.endTransaction()
            } catch (e: Exception) {
                Log.e("DatabaseHelper", "Error ending transaction", e)
            }
        }
    }

    fun getAllFolders(): List<Folder> {
        val folders = mutableListOf<Folder>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_FOLDERS", null)
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
        return folders
    }

    fun saveSetting(key: String, value: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SETTING_KEY, key)
            put(COLUMN_SETTING_VAL, value)
        }
        try {
            db.replace(TABLE_SETTINGS, null, values)
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error saving setting $key", e)
        }
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
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error getting setting $key", e)
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
    }

    companion object {
        private const val DATABASE_NAME = "mega_stream.db"
        private const val DATABASE_VERSION = 2
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
                "$COLUMN_URL TEXT)"

        private const val CREATE_TABLE_SETTINGS = "CREATE TABLE $TABLE_SETTINGS (" +
                "$COLUMN_SETTING_KEY TEXT PRIMARY KEY," +
                "$COLUMN_SETTING_VAL TEXT)"
    }
}
