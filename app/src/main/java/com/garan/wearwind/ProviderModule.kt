package com.garan.wearwind

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides dependencies, in this case, simply the WHS client.
 */
@Module
@InstallIn(SingletonComponent::class)
class ProductionModule {
    @Singleton
    @Provides
    fun providePreferences(@ApplicationContext appContext: Context) =
        Preferences(appContext)
}