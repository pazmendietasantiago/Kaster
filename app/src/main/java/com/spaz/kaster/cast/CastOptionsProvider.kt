package com.spaz.kaster.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId("CC1AD845") // ID de la aplicación Cast por defecto
            .setCastMediaOptions(
                CastMediaOptions.Builder()
                    .setNotificationOptions(
                        NotificationOptions.Builder()
                            .setTargetActivityClassName("com.spaz.kaster.presentation.MainActivity")
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
} 