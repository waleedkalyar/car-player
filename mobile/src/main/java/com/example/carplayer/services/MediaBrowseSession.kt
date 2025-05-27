package com.example.carplayer.services

import MediaBrowseScreen
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.SessionToken
import com.example.carplayer.services.screens.AlertDialogScreen
import com.example.carplayer.shared.database.CarPlayerDatabase
import com.example.carplayer.shared.services.MyMediaService
import kotlinx.coroutines.launch

class MediaBrowseSession() : Session() {

    // We will keep the screen as a mutable variable so that we can update it after getting the media controller.
    private var mediaController: androidx.media3.session.MediaController? = null

    override fun onCreateScreen(intent: Intent): Screen {
        Log.d(CarMediaService.TAG, "onCreateScreen called")

        return AlertDialogScreen(carContext) { screenManager ->

            // Initialize and return the MediaBrowseScreen immediately.
            val mediaBrowseScreen = MediaBrowseScreen(
                carContext,
                CarPlayerDatabase.getInstance(carContext).albumsDao(),
                mediaController // Pass the mediaController, it will be null at first
            )


            // Connect to the media service asynchronously to get the MediaController.
            connectToMediaService(carContext) { controller ->
                mediaController = controller

                // Once the mediaController is ready, update the screen or trigger a screen update.
                // Since the mediaController is ready, we can now update the mediaBrowseScreen.
                mediaBrowseScreen.updateMediaController(controller)
            }

            screenManager.push(mediaBrowseScreen)


        }

       // return mediaBrowseScreen
    }

    // This function asynchronously connects to the media service and retrieves the MediaController.
    private fun connectToMediaService(
        context: CarContext,
        onMediaController: (controller: androidx.media3.session.MediaController) -> Unit
    ) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MyMediaService::class.java)
        )

        lifecycleScope.launch {
            val mediaControllerFuture =
                androidx.media3.session.MediaController.Builder(context, sessionToken).buildAsync()
            mediaControllerFuture.addListener({
                // When the MediaController is ready, invoke the callback with the controller
                onMediaController.invoke(mediaControllerFuture.get())
            }, ContextCompat.getMainExecutor(context))
        }
    }
}
