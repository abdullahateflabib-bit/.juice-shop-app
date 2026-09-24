package com.juiceshop.accounting

import androidx.room.*

@Dao
interface ShopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSale(sale: DailySale)

    @Query("SELECT * FROM daily_sales WHERE date = :date LIMIT 1")
    suspend fun getSaleForDate(date: String): DailySale?

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM daily_sales WHERE monthYear = :monthYear")
    suspend fun getTotalSalesForMonth(monthYear: String): Double

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE date = :date")
    suspend fun getExpensesForDate(date: String): List<Expense>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date = :date")
    suspend fun getTotalExpensesForDate(date: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE monthYear = :monthYear")
    suspend fun getTotalExpensesForMonth(monthYear: String): Double

    @Query("SELECT DISTINCT date FROM daily_sales UNION SELECT DISTINCT date FROM expenses ORDER BY date DESC")
    suspend fun getAllDistinctDates(): List<String>
}

