package com.example.healthapp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise")
    fun getAll(): LiveData<List<ExerciseEntry>>

    @Query("SELECT * FROM exercise")
    fun getAllOnce(): List<ExerciseEntry>

    @Insert
    suspend fun insert(exercise: ExerciseEntry)

    @Update
    suspend fun update(exercise: ExerciseEntry)

    @Delete
    suspend fun delete(exercise: ExerciseEntry)
}
