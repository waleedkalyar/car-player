package com.example.carplayer.services

import android.util.Log
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator


class CarMediaService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    companion object {
        const val TAG = "CarMediaService"
    }

    override fun onCreateSession(): Session {
        Log.d(TAG, "onCreateSession: ")
        return MediaBrowseSession()
    }
}
