package com.example.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class DeviceStorageInfo(
    val totalSpaceBytes: Long,
    val freeSpaceBytes: Long,
    val appUsageBytes: Long
)

object CacheCalculator {
    private val _deviceStorageInfo = MutableStateFlow(DeviceStorageInfo(1L, 1L, 0L))
    val deviceStorageInfo: StateFlow<DeviceStorageInfo> = _deviceStorageInfo.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var isMonitoring = false

    fun startMonitoring(context: Context, scope: CoroutineScope) {
        if (isMonitoring) return
        isMonitoring = true
        scope.launch(Dispatchers.IO) {
            while (true) {
                _isScanning.value = true
                delay(800) // Artificial delay for perceived performance
                calculate(context)
                _isScanning.value = false
                delay(10000) // Update every 10 seconds
            }
        }
    }

    fun forceScan(context: Context, scope: CoroutineScope) {
        if (_isScanning.value) return
        scope.launch(Dispatchers.IO) {
            _isScanning.value = true
            delay(800) // Artificial delay for perceived performance
            calculate(context)
            _isScanning.value = false
        }
    }

    private fun calculate(context: Context) {
        try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val totalSpace = statFs.totalBytes
            val freeSpace = statFs.availableBytes

            var appUsage = 0L
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                appUsage += getFolderSize(cacheDir)
            }
            val filesDir = context.filesDir
            if (filesDir != null && filesDir.exists()) {
                appUsage += getFolderSize(filesDir)
            }
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists()) {
                appUsage += getFolderSize(externalCacheDir)
            }

            _deviceStorageInfo.value = DeviceStorageInfo(
                totalSpaceBytes = totalSpace,
                freeSpaceBytes = freeSpace,
                appUsageBytes = appUsage
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFolderSize(file: File): Long {
        var size: Long = 0
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    size += getFolderSize(child)
                }
            }
        } else {
            size = file.length()
        }
        return size
    }
}
