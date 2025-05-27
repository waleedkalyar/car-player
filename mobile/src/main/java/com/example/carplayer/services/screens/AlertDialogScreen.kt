package com.example.carplayer.services.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class AlertDialogScreen(
    carContext: CarContext,
    private val onContinueClicked: (screenManager: ScreenManager) -> Unit
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("User Acknowledgment and Assumption of Risk")
            .setTitle(MESSAGE)
            .addAction(
                Action.Builder()
                    .setTitle("Continue")
                    .setOnClickListener {
                        onContinueClicked.invoke(screenManager)
                    }
                    .build()
            )
            .setHeaderAction(Action.APP_ICON) // or Action.BACK
            .build()
    }


    companion object {
      private  const val MESSAGE = "By using this application, you acknowledge and agree that you do so entirely at your own risk. The application is provided on an \"as is\" and \"as available\" basis, without any warranties, express or implied, including but not limited to performance, accuracy, or fitness for a particular purpose.\n" +
                "You further agree that the developers, providers, and any affiliated parties shall not be held responsible for any consequences, damages, losses, or liabilities arising from your use of the application. You assume full responsibility for your actions while using the application and accept all associated risks, including but not limited to technical issues, data loss, or other unintended outcomes.\n" +
                "Additionally, you agree to use this application responsibly and ensure that it does not distract you from important tasks, surroundings, or responsibilities. You must always observe safety while using the application and follow all applicable guidelines, laws, precautions, and best practices."
    }
}
