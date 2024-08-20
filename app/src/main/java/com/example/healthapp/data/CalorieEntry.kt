package com.example.healthapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calories")
data class CalorieEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val calories: Float,
    val name: String? = null
)
