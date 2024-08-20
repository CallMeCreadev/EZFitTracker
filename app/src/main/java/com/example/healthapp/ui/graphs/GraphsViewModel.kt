package com.example.healthapp.ui.graphs

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.AppDatabase
import com.example.healthapp.data.HealthRepository
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class GraphsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HealthRepository

    init {
        val healthDao = AppDatabase.getDatabase(application).healthDao()
        repository = HealthRepository(healthDao)
    }

    fun getCombinedData(callback: (List<ILineDataSet>) -> Unit) {
        viewModelScope.launch {
            // Retrieve the isKg preference from shared preferences
            val sharedPreferences = getApplication<Application>().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val isKg = sharedPreferences.getString("weight_unit", "lbs") == "kgs"

            val weightEntries = withContext(Dispatchers.IO) {
                repository.get30DayWeightWithDefaults(isKg).map { Entry(it.day.toTimestamp(), it.value) }
            }
            val calorieEntries = withContext(Dispatchers.IO) {
                repository.get30DayCaloriesWithDefaults().map { Entry(it.day.toTimestamp(), it.value) }
            }
            val exerciseEntries = withContext(Dispatchers.IO) {
                repository.get30DayExerciseMinutesWithDefaults().map { Entry(it.day.toTimestamp(), it.value) }
            }

            val dataSets: MutableList<ILineDataSet> = mutableListOf()

            // Determine color scheme based on the current theme
            val (weightColor, calorieColor, exerciseColor) = getColorsForCurrentTheme()

            if (weightEntries.isNotEmpty()) {
                val weightDataSet = LineDataSet(weightEntries, "Weight Data")
                weightDataSet.color = weightColor
                dataSets.add(weightDataSet)
            }

            if (calorieEntries.isNotEmpty()) {
                val calorieDataSet = LineDataSet(calorieEntries, "Calorie Data")
                calorieDataSet.color = calorieColor
                dataSets.add(calorieDataSet)
            }

            if (exerciseEntries.isNotEmpty()) {
                val exerciseDataSet = LineDataSet(exerciseEntries, "Exercise Data")
                exerciseDataSet.color = exerciseColor
                dataSets.add(exerciseDataSet)
            }

            callback(dataSets)
        }
    }

    private fun getColorsForCurrentTheme(): Triple<Int, Int, Int> {
        // Check if dark mode is active
        val isDarkMode = when (getApplication<Application>().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }

        return if (isDarkMode) {
            // Colors for dark mode: green, white, blue
            Triple(
                android.graphics.Color.GREEN,
                android.graphics.Color.WHITE,
                android.graphics.Color.BLUE
            )
        } else {
            // Colors for light mode: blue, black, red
            Triple(
                android.graphics.Color.BLUE,
                android.graphics.Color.BLACK,
                android.graphics.Color.RED
            )
        }
    }

    private fun String.toTimestamp(): Float {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.parse(this)?.time?.toFloat() ?: 0f
    }
}
