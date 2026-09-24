package com.dayanand.wordscapes.data.repository

import com.dayanand.wordscapes.data.model.Level
import com.dayanand.wordscapes.data.source.LevelDataSource

interface LevelRepository {
    suspend fun getLevels(): List<Level>
    suspend fun getLevel(id: Int): Level?
    suspend fun getTotalLevels(): Int
}

class LevelRepositoryImpl(
    private val dataSource: LevelDataSource
) : LevelRepository {

    override suspend fun getLevels(): List<Level> {
        return dataSource.getLevels()
    }

    override suspend fun getLevel(id: Int): Level? {
        return dataSource.getLevel(id)
    }

    override suspend fun getTotalLevels(): Int {
        return dataSource.getLevels().size
    }
}
