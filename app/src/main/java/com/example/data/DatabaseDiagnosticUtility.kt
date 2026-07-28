package com.example.data

import android.content.Context
import android.util.Log
import net.sqlcipher.Cursor
import net.sqlcipher.database.SQLiteDatabase
import java.io.File

object DatabaseDiagnosticUtility {

    private const val TAG = "DatabaseDiagnostic"

    fun performStartupDiagnostics(context: Context, dbName: String, passphrase: CharArray) {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return

        try {
            SQLiteDatabase.loadLibs(context.applicationContext)
            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                String(passphrase),
                null,
                SQLiteDatabase.OPEN_READWRITE
            )

            // 1) Integrity Check
            val integrityCursor = db.rawQuery("PRAGMA integrity_check;", null)
            var isIntegrityOk = false
            if (integrityCursor.moveToFirst()) {
                val result = integrityCursor.getString(0)
                if (result.equals("ok", ignoreCase = true)) {
                    isIntegrityOk = true
                }
            }
            integrityCursor.close()

            if (!isIntegrityOk) {
                Log.e(TAG, "Integrity check failed. Database might be corrupted.")
                db.close()
                context.deleteDatabase(dbName)
                return
            }

            // 2) Schema Version & Migration
            val schemaCursor = db.rawQuery("PRAGMA schema_version;", null)
            var currentSchemaVersion = -1
            if (schemaCursor.moveToFirst()) {
                currentSchemaVersion = schemaCursor.getInt(0)
            }
            schemaCursor.close()

            Log.i(TAG, "Current schema version: $currentSchemaVersion")
            
            // Database migration utility mechanism
            if (currentSchemaVersion > 0 && currentSchemaVersion < EXPECTED_SCHEMA_VERSION) {
                Log.i(TAG, "Incompatible schema version detected ($currentSchemaVersion). Attempting automatic migration...")
                runMigrationScripts(db, currentSchemaVersion, EXPECTED_SCHEMA_VERSION)
            }

            db.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during database diagnostics: ${e.message}", e)
            context.deleteDatabase(dbName)
        }
    }

    private const val EXPECTED_SCHEMA_VERSION = 1

    private fun runMigrationScripts(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.beginTransaction()
            // In a real app, you'd apply SQL scripts sequentially
            Log.i(TAG, "Applying migration scripts from $oldVersion to $newVersion")
            // Example:
            // if (oldVersion < 2) { db.execSQL("ALTER TABLE user ADD COLUMN new_col TEXT;") }
            
            // Update schema version after successful migration
            db.execSQL("PRAGMA schema_version = $newVersion;")
            db.setTransactionSuccessful()
            Log.i(TAG, "Migration completed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed: ${e.message}", e)
            throw e
        } finally {
            db.endTransaction()
        }
    }
}
