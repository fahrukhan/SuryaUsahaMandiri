package com.rfid.sum.data.room

import androidx.lifecycle.*
import com.rfid.sum.data.room.model.SumApiHistoryModel
import com.rfid.sum.data.room.repository.DaoRepository
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException

class DaoViewModel(private val repository: DaoRepository): ViewModel() {
    val allSumApiHistory: LiveData<List<SumApiHistoryModel>> = repository.allSumApiHistory.asLiveData()

    fun saveApiHistory(history: SumApiHistoryModel)  = viewModelScope.launch {
        repository.saveApiHistory(history)
    }
}

class DaoViewModelFactory(private val repository: DaoRepository): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DaoViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return DaoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}