package com.example.carplayer.shared.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

fun Context.hasStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val readPermission = Manifest.permission.READ_EXTERNAL_STORAGE
        val writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE

        ContextCompat.checkSelfPermission(this, readPermission) == PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(this, writePermission) == PackageManager.PERMISSION_GRANTED)
    } else {
        true // Scoped storage — permission not needed
    }
}

fun Activity.requestStoragePermission(requestCode: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val permissions = mutableListOf<String>().apply {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), requestCode)
    }
}
