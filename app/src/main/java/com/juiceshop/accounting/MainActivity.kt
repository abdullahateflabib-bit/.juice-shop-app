package com.juiceshop.accounting

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.juiceshop.accounting.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private val currencyFormat = DecimalFormat("#,##0.00")

    private val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

    private val currentMonthYear: String
        get() = SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        binding.tvTodayDate.text = "تاريخ اليوم: $todayDate"

        binding.btnAddDailySale.setOnClickListener { showAddDailySaleDialog() }
        binding.btnAddExpense.setOnClickListener { showAddExpenseDialog() }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboardData()
    }

    private fun refreshDashboardData() {
        lifecycleScope.launch {
            val today = todayDate
            val month = currentMonthYear

            val saleObj = db.shopDao().getSaleForDate(today)
            val todaySales = saleObj?.amount ?: 0.0
            val todayExpenses = db.shopDao().getTotalExpensesForDate(today)
            val todayProfit = todaySales - todayExpenses

            val monthSales = db.shopDao().getTotalSalesForMonth(month)
            val monthExpenses = db.shopDao().getTotalExpensesForMonth(month)
            val monthProfit = monthSales - monthExpenses

            binding.tvTodaySales.text = "مبيعات اليوم: ${currencyFormat.format(todaySales)} جنيه"
            binding.tvTodayExpenses.text = "مصروفات اليوم: ${currencyFormat.format(todayExpenses)} جنيه"
            binding.tvTodayProfit.text = "ربح اليوم: ${currencyFormat.format(todayProfit)} جنيه"

            binding.tvMonthSales.text = "إجمالي مبيعات الشهر: ${currencyFormat.format(monthSales)} جنيه"
            binding.tvMonthExpenses.text = "إجمالي مصروفات الشهر: ${currencyFormat.format(monthExpenses)} جنيه"
            binding.tvMonthProfit.text = "صافي ربح الشهر: ${currencyFormat.format(monthProfit)} جنيه"
        }
    }

    private fun showAddDailySaleDialog() {
        val input = EditText(this).apply {
            hint = "أدخل مبيعات اليوم بالجنيه"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        lifecycleScope.launch {
            val existing = db.shopDao().getSaleForDate(todayDate)
            if (existing != null) {
                input.setText(existing.amount.toString())
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("إضافة / تعديل مبيعات اليوم")
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val amount = input.text.toString().trim().toDoubleOrNull()
                if (amount == null || amount < 0) {
                    Toast.makeText(this, "يرجى كتابة رقم صحيح", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    db.shopDao().insertOrUpdateSale(DailySale(todayDate, currentMonthYear, amount))
                    refreshDashboardData()
                    Toast.makeText(this@MainActivity, "تم حفظ المبيعات بنجاح", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun showAddExpenseDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etTitle = EditText(this).apply { hint = "اسم المصروف (مثال: سكر، فواكه)" }
        val etAmount = EditText(this).apply {
            hint = "المبلغ بالجنيه"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val etNote = EditText(this).apply { hint = "ملاحظات (اختياري)" }

        layout.addView(etTitle)
        layout.addView(etAmount)
        layout.addView(etNote)

        MaterialAlertDialogBuilder(this)
            .setTitle("إضافة مصروف جديد")
            .setView(layout)
            .setPositiveButton("إضافة") { _, _ ->
                val title = etTitle.text.toString().trim()
                val amount = etAmount.text.toString().trim().toDoubleOrNull()
                val note = etNote.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(this, "يرجى كتابة اسم المصروف", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "يرجى إدخال مبلغ صحيح", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    val exp = Expense(
                        date = todayDate,
                        monthYear = currentMonthYear,
                        title = title,
                        amount = amount,
                        note = if (note.isNotEmpty()) note else null
                    )
                    db.shopDao().insertExpense(exp)
                    refreshDashboardData()
                    Toast.makeText(this@MainActivity, "تم تسجيل المصروف", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}

