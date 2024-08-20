package com.example.healthapp.ui.entry

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.*
import kotlinx.coroutines.launch

class EntryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HealthRepository

    // Timer related variables
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    var isTimerRunning: Boolean = false
    private set

            init {
                val healthDao = AppDatabase.getDatabase(application).healthDao()
                repository = HealthRepository(healthDao)
            }

    // Timer functions
    fun startTimer() {
        if (!isTimerRunning) {
            startTime = SystemClock.elapsedRealtime() - elapsedTime
            isTimerRunning = true
        }
    }

    fun stopTimer() {
        if (isTimerRunning) {
            elapsedTime = SystemClock.elapsedRealtime() - startTime
            isTimerRunning = false
        }
    }

    fun resetTimer() {
        elapsedTime = 0L
        startTime = 0L
        isTimerRunning = false
    }

    fun getElapsedTime(): Long {
        return if (isTimerRunning) {
            SystemClock.elapsedRealtime() - startTime
        } else {
            elapsedTime
        }
    }

    fun saveElapsedTime(): Float {
        val minutes = elapsedTime / 60000f
        resetTimer() // Reset timer after saving
        return minutes
    }

    fun formatElapsedTime(elapsedTime: Long): String {
        val hours = (elapsedTime / (1000 * 60 * 60)).toInt()
        val minutes = ((elapsedTime / (1000 * 60)) % 60).toInt()
        val seconds = ((elapsedTime / 1000) % 60).toInt()

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Existing functions
    fun insert(weight: WeightEntry, isKg: Boolean) {
        viewModelScope.launch {
            Log.d("EntryViewModel", "Inserting weight: ${weight.weight} ${if (isKg) "kg" else "lbs"}")
            repository.insert(weight, isKg)
            Log.d("EntryViewModel", "Weight insert operation completed")
        }
    }

    fun insert(calorie: CalorieEntry) {
        viewModelScope.launch {
            repository.insert(calorie)
        }
    }

    fun insert(exercise: ExerciseEntry) {
        viewModelScope.launch {
            repository.insert(exercise)
        }
    }

    fun processCalorieEntry(input: String) {
        viewModelScope.launch {
            val calories = input.toFloatOrNull()
            if (calories != null) {
                // Direct calorie value input, create a new entry
                val calorieEntry =
                    CalorieEntry(calories = calories, timestamp = System.currentTimeMillis())
                repository.insert(calorieEntry)
            } else {
                // Check for existing entry by name
                val existingEntry = repository.getMostRecentNonZeroCalorieByName(input)
                if (existingEntry != null) {
                    // Use the most recent non-zero calorie entry's value
                    val calorieEntry = CalorieEntry(
                        name = input,
                        calories = existingEntry.calories,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insert(calorieEntry)
                } else {
                    // No existing entry found, create a new entry with zero calories
                    val calorieEntry = CalorieEntry(
                        name = input,
                        calories = 0f,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insert(calorieEntry)
                }
            }
        }
    }
}
