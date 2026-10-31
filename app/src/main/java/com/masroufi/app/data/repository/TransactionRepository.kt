package com.masroufi.app.data.repository

import com.masroufi.app.data.local.TransactionDao
import com.masroufi.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class TransactionRepository(private val dao: TransactionDao) {

    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val pendingQuestions: Flow<List<TransactionEntity>> = dao.getPendingQuestions()

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return dao.insertTransaction(transaction)
    }

    suspend fun updateNecessity(id: Long, necessity: ExpenseNecessity) {
        dao.updateNecessity(id, necessity)
    }

    suspend fun attachReceipt(id: Long, receiptUri: String, necessity: ExpenseNecessity, notes: String) {
        dao.attachReceiptAndClassification(id, receiptUri, necessity, notes)
    }

    suspend fun deleteTransaction(id: Long) {
        dao.deleteTransaction(id)
    }

    /**
     * حساب ملخص الهدر المالي للشهر الحالي
     */
    fun getCurrentMonthWasteSummary(): Flow<MonthlyWasteSummary> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        return dao.getTransactionsForPeriod(startOfMonth, endOfMonth).map { list ->
            var income = 0.0
            var totalExpense = 0.0
            var essentialExpense = 0.0
            var wasteExpense = 0.0
            var pendingCount = 0

            for (tx in list) {
                when (tx.type) {
                    TransactionType.INCOMING_TRANSFER -> {
                        income += tx.amount
                    }
                    TransactionType.OUTGOING_TRANSFER,
                    TransactionType.PURCHASE_POS,
                    TransactionType.PURCHASE_ONLINE,
                    TransactionType.ATM_WITHDRAWAL -> {
                        totalExpense += tx.amount
                        when (tx.necessity) {
                            ExpenseNecessity.ESSENTIAL -> essentialExpense += tx.amount
                            ExpenseNecessity.NON_ESSENTIAL -> wasteExpense += tx.amount
                            ExpenseNecessity.PENDING_REVIEW -> {
                                pendingCount++
                                // نعتبر مؤقتاً نصف المصروف غير المؤكد هدر محتمل حتى يسأل المستخدم
                                wasteExpense += (tx.amount * 0.5)
                            }
                            ExpenseNecessity.NEUTRAL -> { /* تحويلات محايدة */ }
                        }
                    }
                    TransactionType.UNKNOWN -> {}
                }
            }

            val wastePercentage = if (totalExpense > 0) (wasteExpense / totalExpense) * 100.0 else 0.0
            val potentialSavings = wasteExpense

            MonthlyWasteSummary(
                totalIncome = income,
                totalExpense = totalExpense,
                essentialExpenses = essentialExpense,
                wasteExpenses = wasteExpense,
                wastePercentage = wastePercentage,
                potentialMonthlySavings = potentialSavings,
                pendingQuestionsCount = pendingCount
            )
        }
    }
}
