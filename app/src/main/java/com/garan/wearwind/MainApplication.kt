package com.garan.wearwind

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.garan.wearwind.SettingsRepository.Companion.PREFERENCES_FILENAME
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application()

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(PREFERENCES_FILENAME)