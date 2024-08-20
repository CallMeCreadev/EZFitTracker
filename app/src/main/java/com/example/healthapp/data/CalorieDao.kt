package com.example.healthapp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface CalorieDao {
    @Query("SELECT * FROM calories")
    fun getAll(): LiveData<List<CalorieEntry>>

    @Query("SELECT * FROM calories")
    fun getAllOnce(): List<CalorieEntry>

    @Insert
    suspend fun insert(calorie: CalorieEntry)

    @Update
    suspend fun update(calorie: CalorieEntry)

    @Delete
    suspend fun delete(calorie: CalorieEntry)

    @Query("SELECT * FROM calories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getByName(name: String): CalorieEntry?
}
