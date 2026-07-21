package com.dennyfid.golfswingrecorder.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val MOTION_THRESHOLD = doublePreferencesKey("motion_threshold")
    private val PRE_ROLL_SEC = intPreferencesKey("pre_roll_sec")

    val motionThreshold: Flow<Double> = context.dataStore.data.map { it[MOTION_THRESHOLD] ?: 25.0 }
    val preRollSec: Flow<Int> = context.dataStore.data.map { it[PRE_ROLL_SEC] ?: 1 }

    suspend fun setMotionThreshold(threshold: Double) {
        context.dataStore.edit { it[MOTION_THRESHOLD] = threshold }
    }
}
