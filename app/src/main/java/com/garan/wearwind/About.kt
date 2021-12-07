package com.garan.wearwind

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.wear.remote.interactions.RemoteActivityHelper
import kotlinx.coroutines.guava.await

suspend fun launchAbout(context: Context) {
    val helper = RemoteActivityHelper(context)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.about_url))).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    try {
        helper.startRemoteActivity(intent).await()
    } catch (e: RemoteActivityHelper.RemoteIntentException) {
        Log.w(TAG, "Error starting remote intent: " + e.localizedMessage)
    }
}
