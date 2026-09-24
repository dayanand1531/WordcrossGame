package com.dayanand.wordscapes.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserPreferencesRepository {
    val unlockedLevelFlow: Flow<Int>
    val completedLevelsFlow: Flow<Set<Int>>
    val totalScoreFlow: Flow<Int>
    suspend fun completeLevel(levelId: Int, nextLevelId: Int, scoreGained: Int)
    suspend fun updateScore(newScore: Int)
}

class UserPreferencesRepositoryImpl(private val context: Context) : UserPreferencesRepository {

    private object PreferencesKeys {
        val UNLOCKED_LEVEL = intPreferencesKey("unlocked_level")
        val COMPLETED_LEVELS = stringSetPreferencesKey("completed_levels")
        val TOTAL_SCORE = intPreferencesKey("total_score")
    }

    override val unlockedLevelFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.UNLOCKED_LEVEL] ?: 1
    }

    override val completedLevelsFlow: Flow<Set<Int>> = context.dataStore.data.map { preferences ->
        val stringSet = preferences[PreferencesKeys.COMPLETED_LEVELS] ?: emptySet()
        stringSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    override val totalScoreFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TOTAL_SCORE] ?: 0
    }

    override suspend fun completeLevel(levelId: Int, nextLevelId: Int, scoreGained: Int) {
        context.dataStore.edit { preferences ->
            val currentCompleted = preferences[PreferencesKeys.COMPLETED_LEVELS] ?: emptySet()
            preferences[PreferencesKeys.COMPLETED_LEVELS] = currentCompleted + levelId.toString()

            val currentUnlocked = preferences[PreferencesKeys.UNLOCKED_LEVEL] ?: 1
            if (nextLevelId > currentUnlocked) {
                preferences[PreferencesKeys.UNLOCKED_LEVEL] = nextLevelId
            }

            preferences[PreferencesKeys.TOTAL_SCORE] = scoreGained
        }
    }

    override suspend fun updateScore(newScore: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOTAL_SCORE] = newScore
        }
    }
}
