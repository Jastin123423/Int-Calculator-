package com.example.data.repository

import com.example.data.database.HistoryDao
import com.example.data.database.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {

    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val favorites: Flow<List<HistoryEntity>> = historyDao.getFavorites()

    fun search(query: String): Flow<List<HistoryEntity>> = historyDao.searchHistory(query)

    suspend fun insert(expression: String, result: String, category: String = "Standard"): Long {
        return historyDao.insertHistory(
            HistoryEntity(
                expression = expression,
                result = result,
                category = category
            )
        )
    }

    suspend fun toggleFavorite(item: HistoryEntity) {
        historyDao.updateHistory(item.copy(isFavorite = !item.isFavorite))
    }

    suspend fun deleteById(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun clearAll() {
        historyDao.clearAllHistory()
    }
}
