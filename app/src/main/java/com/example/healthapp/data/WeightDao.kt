package com.example.healthapp.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight")
    fun getAll(): LiveData<List<WeightEntry>>

    @Query("SELECT * FROM weight")
    fun getAllOnce(): List<WeightEntry>

    @Insert
    suspend fun insert(weight: WeightEntry)

    @Update
    suspend fun update(weight: WeightEntry)

    @Delete
    suspend fun delete(weight: WeightEntry)
}
