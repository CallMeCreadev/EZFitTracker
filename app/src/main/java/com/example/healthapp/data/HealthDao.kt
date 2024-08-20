package com.example.healthapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface HealthDao {

    @Query("DELETE FROM calories WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldCalorieRecords(cutoffTimestamp: Long): Int

    @Query("DELETE FROM exercise WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldExerciseRecords(cutoffTimestamp: Long): Int

    @Query("DELETE FROM weight WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldWeightRecords(cutoffTimestamp: Long): Int

    @Query("SELECT * FROM weight ORDER BY timestamp DESC")
    fun getAllWeights(): List<WeightEntry>

    @Query("SELECT * FROM calories ORDER BY timestamp DESC")
    fun getAllCalories(): List<CalorieEntry>

    @Query("SELECT * FROM exercise ORDER BY timestamp DESC")
    fun getAllExercises(): List<ExerciseEntry>

    @Insert
    suspend fun insertWeight(weight: WeightEntry)

    @Insert
    suspend fun insertCalorie(calorie: CalorieEntry)

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntry)

    @Update
    suspend fun updateWeight(weight: WeightEntry)

    @Update
    suspend fun updateCalorie(calorie: CalorieEntry)

    @Update
    suspend fun updateExercise(exercise: ExerciseEntry)

    @Delete
    suspend fun deleteWeight(weight: WeightEntry)

    @Delete
    suspend fun deleteCalorie(calorie: CalorieEntry)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntry)

    @Query("SELECT * FROM calories WHERE name = :name AND calories > 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentNonZeroCalorieByName(name: String): CalorieEntry?

    @Query("SELECT timestamp FROM calories WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    suspend fun getTimestampsForLast7Days(startTime: Long, endTime: Long): List<Long>


    @Query("UPDATE calories SET calories = :calories WHERE name = :name")
    suspend fun updateNamesInCalories(name: String, calories: Int)

    @Query("SELECT SUM(calories) FROM calories WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getCaloriesSum(startTime: Long, endTime: Long): Float

    @Query("SELECT SUM(minutes) FROM exercise WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getExerciseMinutesSum(startTime: Long, endTime: Long): Float

    @Query("SELECT AVG(weight) FROM weight WHERE timestamp BETWEEN :start AND :end")
    suspend fun getWeightAverage(start: Long, end: Long): Float

    @Query("SELECT COUNT(DISTINCT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch'))) FROM calories WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getDistinctDaysWithData(startTime: Long, endTime: Long): Int

    @Query("SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch')) as day, SUM(calories) as value FROM calories WHERE timestamp BETWEEN :startTime AND :endTime GROUP BY day ORDER BY day")
    suspend fun getDailyCaloriesSum(startTime: Long, endTime: Long): List<DailyValue>

    @Query("SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch')) as day, SUM(minutes) as value FROM exercise WHERE timestamp BETWEEN :startTime AND :endTime GROUP BY day ORDER BY day")
    suspend fun getDailyExerciseMinutesSum(startTime: Long, endTime: Long): List<DailyValue>

    @Query("SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch')) as day, AVG(weight) as value FROM weight WHERE timestamp BETWEEN :start AND :end GROUP BY day ORDER BY day")
    suspend fun getDailyWeightAverage(start: Long, end: Long): List<DailyValue>
}
