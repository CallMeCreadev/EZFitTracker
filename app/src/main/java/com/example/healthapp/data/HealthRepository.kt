package com.example.healthapp.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class HealthRepository(private val healthDao: HealthDao) {

    private val _allWeights = MutableLiveData<List<WeightEntry>>()
    val allWeights: LiveData<List<WeightEntry>> = _allWeights

    private val _allCalories = MutableLiveData<List<CalorieEntry>>()
    val allCalories: LiveData<List<CalorieEntry>> = _allCalories

    private val _allExercises = MutableLiveData<List<ExerciseEntry>>()
    val allExercises: LiveData<List<ExerciseEntry>> = _allExercises

    suspend fun refreshData() {
        withContext(Dispatchers.IO) {
            _allWeights.postValue(healthDao.getAllWeights())
            _allCalories.postValue(healthDao.getAllCalories())
            _allExercises.postValue(healthDao.getAllExercises())
        }
    }

    suspend fun insert(weight: WeightEntry, isKg: Boolean = false) {
        withContext(Dispatchers.IO) {
            val weightInLbs = if (isKg) convertKgToLbs(weight.weight) else weight.weight
            Log.d("HealthRepository", "Inserting weight in lbs: $weightInLbs")
            healthDao.insertWeight(weight.copy(weight = weightInLbs))
            Log.d("HealthRepository", "Weight insert operation in DAO completed")
            refreshData()
        }
    }

    suspend fun insert(calorie: CalorieEntry) {
        withContext(Dispatchers.IO) {
            healthDao.insertCalorie(calorie)
            refreshData()
        }
    }

    suspend fun insert(exercise: ExerciseEntry) {
        withContext(Dispatchers.IO) {
            healthDao.insertExercise(exercise)
            refreshData()
        }
    }

    private fun convertKgToLbs(weightKg: Float): Float {
        return String.format("%.2f", weightKg * 2.20462f).toFloat()
    }

    private fun convertLbsToKg(weightLbs: Float): Float {
        return String.format("%.2f", weightLbs * 0.453592f).toFloat()
    }


    suspend fun getMostRecentNonZeroCalorieByName(name: String): CalorieEntry? {
        return withContext(Dispatchers.IO) {
            healthDao.getMostRecentNonZeroCalorieByName(name)
        }
    }


    suspend fun getAllWeightsOnce(isKg: Boolean = false): List<WeightEntry> {
        return withContext(Dispatchers.IO) {
            val weights = healthDao.getAllWeights()
            if (isKg) {
                weights.map { it.copy(weight = convertLbsToKg(it.weight)) }
            } else {
                weights
            }
        }
    }

    suspend fun getAllCaloriesOnce(): List<CalorieEntry> {
        return withContext(Dispatchers.IO) {
            healthDao.getAllCalories()
        }
    }

    suspend fun getAllExercisesOnce(): List<ExerciseEntry> {
        return withContext(Dispatchers.IO) {
            healthDao.getAllExercises()
        }
    }

    suspend fun update(weight: WeightEntry, isKg: Boolean = false) {
        withContext(Dispatchers.IO) {
            val weightInLbs = if (isKg) convertKgToLbs(weight.weight) else weight.weight
            healthDao.updateWeight(weight.copy(weight = weightInLbs))
            refreshData()
        }
    }
    suspend fun update(calorie: CalorieEntry) {
        withContext(Dispatchers.IO) {
            healthDao.updateCalorie(calorie)
            refreshData()
        }
    }

    suspend fun update(exercise: ExerciseEntry) {
        withContext(Dispatchers.IO) {
            healthDao.updateExercise(exercise)
            refreshData()
        }
    }

    suspend fun updateNamesInCalories(name: String, calories: Int) {
        withContext(Dispatchers.IO) {
            healthDao.updateNamesInCalories(name, calories)
            refreshData()
        }
    }

    suspend fun delete(weight: WeightEntry) {
        withContext(Dispatchers.IO) {
            healthDao.deleteWeight(weight)
            refreshData()
        }
    }

    suspend fun delete(calorie: CalorieEntry) {
        withContext(Dispatchers.IO) {
            healthDao.deleteCalorie(calorie)
            refreshData()
        }
    }

    suspend fun delete(exercise: ExerciseEntry) {
        withContext(Dispatchers.IO) {
            healthDao.deleteExercise(exercise)
            refreshData()
        }
    }

    private val oneDayMillis: Long = 24 * 60 * 60 * 1000

    suspend fun get7DayCaloriesAverage(): Pair<Float, Float> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val sevenDaysAgo = currentTime - 7 * oneDayMillis
            val fourteenDaysAgo = currentTime - 14 * oneDayMillis

            // Get timestamps for the last 7 days for debugging
            //val last7DaysTimestamps = healthDao.getTimestampsForLast7Days(sevenDaysAgo, currentTime)
            //Log.d("TimestampsDebug", "Timestamps in the last 7 days: $last7DaysTimestamps")

            val last7DaysCalories = getSimplifiedAverage(
                sevenDaysAgo,
                currentTime,
                { start, end -> healthDao.getCaloriesSum(start, end) }
            )

            val previous7DaysCalories = getSimplifiedAverage(
                fourteenDaysAgo,
                sevenDaysAgo,
                { start, end -> healthDao.getCaloriesSum(start, end) }
            )

            Pair(last7DaysCalories, previous7DaysCalories)
        }
    }
    // Function for 30-day Calories Average
    suspend fun get30DayCaloriesAverage(): Pair<Float, Float> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val thirtyDaysAgo = currentTime - 30 * oneDayMillis
            val sixtyDaysAgo = currentTime - 60 * oneDayMillis

            val last30DaysCalories = getSimplifiedAverage(
                thirtyDaysAgo,
                currentTime,
                { start, end -> healthDao.getCaloriesSum(start, end) }
            )

            val previous30DaysCalories = getSimplifiedAverage(
                sixtyDaysAgo,
                thirtyDaysAgo,
                { start, end -> healthDao.getCaloriesSum(start, end) }
            )

            Pair(last30DaysCalories, previous30DaysCalories)
        }
    }

    // Function for 7-day Exercise Minutes Average
    suspend fun get7DayExerciseMinutesAverage(): Pair<Float, Float> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val sevenDaysAgo = currentTime - 7 * oneDayMillis
            val fourteenDaysAgo = currentTime - 14 * oneDayMillis

            val last7DaysMinutes = getSimplifiedAverage(
                sevenDaysAgo,
                currentTime,
                { start, end -> healthDao.getExerciseMinutesSum(start, end) }
            )

            val previous7DaysMinutes = getSimplifiedAverage(
                fourteenDaysAgo,
                sevenDaysAgo,
                { start, end -> healthDao.getExerciseMinutesSum(start, end) }
            )

            Pair(last7DaysMinutes, previous7DaysMinutes)
        }
    }

    // Function for 30-day Exercise Minutes Average
    suspend fun get30DayExerciseMinutesAverage(): Pair<Float, Float> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val thirtyDaysAgo = currentTime - 30 * oneDayMillis
            val sixtyDaysAgo = currentTime - 60 * oneDayMillis

            val last30DaysMinutes = getSimplifiedAverage(
                thirtyDaysAgo,
                currentTime,
                { start, end -> healthDao.getExerciseMinutesSum(start, end) }
            )

            val previous30DaysMinutes = getSimplifiedAverage(
                sixtyDaysAgo,
                thirtyDaysAgo,
                { start, end -> healthDao.getExerciseMinutesSum(start, end) }
            )

            Pair(last30DaysMinutes, previous30DaysMinutes)
        }
    }
    suspend fun get7DayWeightAverage(isKg: Boolean = false): Pair<Float, Float> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val sevenDaysAgo = currentTime - 7 * oneDayMillis
            val fourteenDaysAgo = currentTime - 14 * oneDayMillis

            val last7DaysWeight = getSimplifiedWeightAverage(
                sevenDaysAgo,
                currentTime,
                { start, end ->
                    val avg = healthDao.getWeightAverage(start, end)
                    if (isKg) convertLbsToKg(avg) else avg
                }
            )

            val previous7DaysWeight = getSimplifiedWeightAverage(
                fourteenDaysAgo,
                sevenDaysAgo,
                { start, end ->
                    val avg = healthDao.getWeightAverage(start, end)
                    if (isKg) convertLbsToKg(avg) else avg
                }
            )

            Pair(last7DaysWeight, previous7DaysWeight)
        }
    }

    suspend fun get30DayWeightAverage(isKg: Boolean = false): Pair<Float, Float> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val thirtyDaysAgo = currentTime - 30 * oneDayMillis
            val sixtyDaysAgo = currentTime - 60 * oneDayMillis

            val last30DaysWeight = getSimplifiedWeightAverage(
                thirtyDaysAgo,
                currentTime,
                { start, end ->
                    val avg = healthDao.getWeightAverage(start, end)
                    if (isKg) convertLbsToKg(avg) else avg
                }
            )

            val previous30DaysWeight = getSimplifiedWeightAverage(
                sixtyDaysAgo,
                thirtyDaysAgo,
                { start, end ->
                    val avg = healthDao.getWeightAverage(start, end)
                    if (isKg) convertLbsToKg(avg) else avg
                }
            )

            Pair(last30DaysWeight, previous30DaysWeight)
        }
    }

    private suspend fun getSimplifiedAverage(
        startTime: Long,
        endTime: Long,
        sumFunction: suspend (Long, Long) -> Float
    ): Float {
        val totalSum = sumFunction(startTime, endTime)
        Log.d("AverageCalculation", "startTime: $startTime End Time: $endTime")
        val daysWithData = healthDao.getDistinctDaysWithData(startTime, endTime)
        val totalDays = ((endTime - startTime) / oneDayMillis).toInt()

        Log.d("AverageCalculation", "Total Sum: $totalSum")
        Log.d("AverageCalculation", "Days with Data: $daysWithData")
        Log.d("AverageCalculation", "Total Days: $totalDays")

        return if (daysWithData == 0) {
            0f // No data at all
        } else {
            val averageWithData = totalSum / daysWithData
            val missingDays = totalDays - daysWithData

            Log.d("AverageCalculation", "Average with Data: $averageWithData")
            Log.d("AverageCalculation", "Missing Days: $missingDays")

            val adjustedSum = totalSum + (missingDays * averageWithData)
            Log.d("AverageCalculation", "Adjusted Sum: $adjustedSum")

            adjustedSum / totalDays
        }
    }

    suspend fun deleteRecordsOlderThan61Days() {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - 61L * 24 * 60 * 60 * 1000 // 61 days in milliseconds
        Log.d("HealthRepository", "Deleting records older than: ${Date(cutoffTime)}")

        val deletedCalories = healthDao.deleteOldCalorieRecords(cutoffTime)
        val deletedExercises = healthDao.deleteOldExerciseRecords(cutoffTime)
        val deletedWeights = healthDao.deleteOldWeightRecords(cutoffTime)

        Log.d("HealthRepository", "Deleted $deletedCalories calorie records")
        Log.d("HealthRepository", "Deleted $deletedExercises exercise records")
        Log.d("HealthRepository", "Deleted $deletedWeights weight records")
    }

    private suspend fun getSimplifiedWeightAverage(
        startTime: Long,
        endTime: Long,
        averageFunction: suspend (Long, Long) -> Float
    ): Float {
        val totalDays = ((endTime - startTime) / oneDayMillis).toInt()
        val daysWithData = healthDao.getDistinctDaysWithData(startTime, endTime)

        Log.d("AverageCalculation", "Total Days: $totalDays")
        Log.d("AverageCalculation", "Days with Data: $daysWithData")

        return if (daysWithData == 0) {
            0f // No data at all
        } else {
            // Fetch the average weight across the days
            val averageWeight = averageFunction(startTime, endTime)

            Log.d("AverageCalculation", "Average Weight from Data: $averageWeight")

            // Calculate the average by distributing the averageWeight over the total period
            averageWeight
        }
    }

    // Repeat similar functions for exercise minutes and weight

    suspend fun get30DayCaloriesWithDefaults(): List<DailyValue> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val thirtyDaysAgo = currentTime - 30 * oneDayMillis

            val dailyValues = healthDao.getDailyCaloriesSum(thirtyDaysAgo, currentTime)

            fillMissingDaysWithAverage(dailyValues, thirtyDaysAgo, currentTime)
        }
    }

    suspend fun get30DayExerciseMinutesWithDefaults(): List<DailyValue> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val thirtyDaysAgo = currentTime - 30 * oneDayMillis

            val dailyValues = healthDao.getDailyExerciseMinutesSum(thirtyDaysAgo, currentTime)

            fillMissingDaysWithAverage(dailyValues, thirtyDaysAgo, currentTime)
        }
    }

    suspend fun get30DayWeightWithDefaults(isKg: Boolean = false): List<DailyValue> {
        return withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val thirtyDaysAgo = currentTime - 30 * oneDayMillis

            val dailyValues = healthDao.getDailyWeightAverage(thirtyDaysAgo, currentTime)

            fillMissingDaysWithAverage(dailyValues, thirtyDaysAgo, currentTime).map {
                if (isKg) it.copy(value = convertLbsToKg(it.value)) else it
            }
        }
    }

    private fun fillMissingDaysWithAverage(
        dailyValues: List<DailyValue>,
        startTime: Long,
        endTime: Long
    ): List<DailyValue> {
        val daysMap = dailyValues.associateBy { it.day }.toMutableMap()

        val calendar = Calendar.getInstance().apply { timeInMillis = startTime }
        val totalDays = (endTime - startTime) / oneDayMillis

        for (i in 0..totalDays) {
            val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            if (!daysMap.containsKey(day)) {
                val average = dailyValues.map { it.value }.average().toFloat()
                daysMap[day] = DailyValue(day, average)
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return daysMap.values.sortedBy { it.day }
    }



}
