package com.example.healthapp.ui.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.*
import kotlinx.coroutines.launch

enum class DatabaseCategory {
    WEIGHT, CALORIES, EXERCISE
}

class DatabaseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HealthRepository
    private val _entries = MutableLiveData<List<Any>>()
    val entries: LiveData<List<Any>> = _entries

    init {
        val healthDao = AppDatabase.getDatabase(application).healthDao()
        repository = HealthRepository(healthDao)
    }

    private fun isKg(): Boolean {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        return sharedPreferences.getString("weight_unit", "lbs") == "kgs"
    }

    fun setCategory(category: DatabaseCategory) {
        viewModelScope.launch {
            _entries.postValue(when (category) {
                DatabaseCategory.WEIGHT -> repository.getAllWeightsOnce(isKg())
                DatabaseCategory.CALORIES -> repository.getAllCaloriesOnce()
                DatabaseCategory.EXERCISE -> repository.getAllExercisesOnce()
            })
        }
    }

    fun clearCategory() {
        _entries.postValue(emptyList())
    }

    fun delete(item: Any) {
        viewModelScope.launch {
            when (item) {
                is WeightEntry -> repository.delete(item)
                is CalorieEntry -> repository.delete(item)
                is ExerciseEntry -> repository.delete(item)
            }
            refreshData()
        }
    }

    fun update(item: Any) {
        viewModelScope.launch {
            when (item) {
                is WeightEntry -> repository.update(item, isKg())
                is CalorieEntry -> repository.update(item)
                is ExerciseEntry -> repository.update(item)
            }
            refreshData()
        }
    }

    fun updateCalorieName(item: CalorieEntry, newName: String) {
        viewModelScope.launch {
            val updatedItem = item.copy(name = newName)
            repository.update(updatedItem)
            refreshData()
        }
    }

    fun updateNamesInCalories(name: String, calories: Int) {
        viewModelScope.launch {
            repository.updateNamesInCalories(name, calories)
            refreshData()
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            when (_entries.value?.firstOrNull()) {
                is WeightEntry -> _entries.postValue(repository.getAllWeightsOnce(isKg()))
                is CalorieEntry -> _entries.postValue(repository.getAllCaloriesOnce())
                is ExerciseEntry -> _entries.postValue(repository.getAllExercisesOnce())
            }
        }
    }
}
