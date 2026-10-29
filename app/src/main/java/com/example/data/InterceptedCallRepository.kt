package com.example.data

import kotlinx.coroutines.flow.Flow

class InterceptedCallRepository(private val dao: InterceptedCallDao) {
    val allCalls: Flow<List<InterceptedCall>> = dao.getAllInterceptedCalls()

    suspend fun insert(call: InterceptedCall): Long {
        return dao.insertCall(call)
    }

    suspend fun update(call: InterceptedCall) {
        dao.updateCall(call)
    }

    suspend fun delete(call: InterceptedCall) {
        dao.deleteCall(call)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun getCallById(id: Long): InterceptedCall? {
        return dao.getCallById(id)
    }
}
