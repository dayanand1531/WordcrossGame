package com.dayanand.wordscapes.data.source

import android.content.Context
import com.dayanand.wordscapes.data.model.Level
import com.dayanand.wordscapes.data.model.LevelsData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class LevelDataSource(private val context: Context) {

    private val gson = Gson()
    private var cachedLevels: List<Level>? = null

    suspend fun getLevels(): List<Level> = withContext(Dispatchers.IO) {
        cachedLevels?.let { return@withContext it }

        try {
            context.assets.open("levels.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val data = gson.fromJson(reader, LevelsData::class.java)
                    val levels = data?.levels ?: emptyList()
                    cachedLevels = levels
                    levels
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getLevel(id: Int): Level? {
        return getLevels().find { it.id == id }
    }
}
