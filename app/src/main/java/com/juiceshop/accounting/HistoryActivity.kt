package com.juiceshop.accounting

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class HistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val currencyFormat = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val listView = ListView(this)
        setContentView(listView)
        title = "سجل الأيام السابقة"

        db = AppDatabase.getDatabase(this)
        loadHistory(listView)
    }

    private fun loadHistory(listView: ListView) {
        lifecycleScope.launch {
            val dates = db.shopDao().getAllDistinctDates()
            val listItems = mutableListOf<String>()

            for (date in dates) {
                val sale = db.shopDao().getSaleForDate(date)?.amount ?: 0.0
                val expense = db.shopDao().getTotalExpensesForDate(date)
                val profit = sale - expense
                listItems.add("📅 $date\nالمبيعات: ${currencyFormat.format(sale)} ج | المصروفات: ${currencyFormat.format(expense)} ج\nالربح: ${currencyFormat.format(profit)} جنيه")
            }

            listView.adapter = ArrayAdapter(this@HistoryActivity, android.R.layout.simple_list_item_1, listItems)

            listView.setOnItemClickListener { _, _, position, _ ->
                val selectedDate = dates[position]
                showExpensesForDateDialog(selectedDate, listView)
            }
        }
    }

    private fun showExpensesForDateDialog(date: String, listView: ListView) {
        lifecycleScope.launch {
            val expenses = db.shopDao().getExpensesForDate(date)
            if (expenses.isEmpty()) {
                Toast.makeText(this@HistoryActivity, "لا توجد مصروفات مسجلة لهذا اليوم", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val items = expenses.map { "${it.title}: ${currencyFormat.format(it.amount)} جنيه ${it.note?.let { n -> "($n)" } ?: ""}" }.toTypedArray()

            MaterialAlertDialogBuilder(this@HistoryActivity)
                .setTitle("مصروفات يوم: $date (اضغط للحذف)")
                .setItems(items) { _, which ->
                    val exp = expenses[which]
                    showDeleteExpenseConfirm(exp, listView)
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }
    }

    private fun showDeleteExpenseConfirm(expense: Expense, listView: ListView) {
        MaterialAlertDialogBuilder(this)
            .setTitle("تأكيد الحذف")
            .setMessage("هل تريد حذف مصروف '${expense.title}' بقيمة ${expense.amount} جنيه؟")
            .setPositiveButton("نعم، حذف") { _, _ ->
                lifecycleScope.launch {
                    db.shopDao().deleteExpense(expense)
                    Toast.makeText(this@HistoryActivity, "تم حذف المصروف بنجاح", Toast.LENGTH_SHORT).show()
                    loadHistory(listView)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}

