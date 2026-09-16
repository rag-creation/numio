package com.rr.numio.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {

    val history: Flow<List<HistoryEntity>> = dao.getAll()

    suspend fun save(expression: String, result: String) {
        dao.insert(HistoryEntity(expression = expression, result = result))
    }

    suspend fun clear() {
        dao.clearAll()
    }
}