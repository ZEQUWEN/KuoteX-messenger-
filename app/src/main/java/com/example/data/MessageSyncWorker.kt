package com.example.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import android.util.Log

class MessageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("MessageSyncWorker", "Starting background sync...")
        return try {
            // In a real app, you would fetch missed messages from a REST endpoint here,
            // since WebSockets are usually closed in the background.
            // For example:
            // val newMessages = api.getMissedMessages(lastSyncTime)
            // repository.saveMessages(newMessages)
            
            Log.d("MessageSyncWorker", "Background sync completed.")
            Result.success()
        } catch (e: Exception) {
            Log.e("MessageSyncWorker", "Background sync failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
