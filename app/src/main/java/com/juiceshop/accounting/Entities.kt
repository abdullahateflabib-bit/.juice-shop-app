package com.juiceshop.accounting

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_sales")
data class DailySale(
    @PrimaryKey
    val date: String,
    val monthYear: String,
    val amount: Double
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val monthYear: String,
    val title: String,
    val amount: Double,
    val note: String? = null
)
