package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.InterceptedCall
import com.example.data.InterceptedCallRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InterceptorViewModel(private val repository: InterceptedCallRepository) : ViewModel() {

    val allCalls: StateFlow<List<InterceptedCall>> = repository.allCalls
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertCall(decodedCode: String, actionType: String, notes: String = "") {
        android.util.Log.i("USSD_CAPTURED", "=================== DIAL INTERCEPTOR (SIMULATED) ===================")
        android.util.Log.i("USSD_CAPTURED", "[SIMULATED] >>> $decodedCode <<<")
        android.util.Log.i("USSD_CAPTURED", "Action Type : $actionType")
        android.util.Log.i("USSD_CAPTURED", "====================================================================")

        viewModelScope.launch {
            repository.insert(
                InterceptedCall(
                    rawUri = "tel:${java.net.URLEncoder.encode(decodedCode, "UTF-8")}",
                    decodedCode = decodedCode,
                    actionType = actionType,
                    notes = notes
                )
            )
        }
    }

    fun deleteCall(call: InterceptedCall) {
        viewModelScope.launch {
            repository.delete(call)
        }
    }

    fun updateCall(call: InterceptedCall) {
        viewModelScope.launch {
            repository.update(call)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}

class InterceptorViewModelFactory(private val repository: InterceptedCallRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InterceptorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InterceptorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
