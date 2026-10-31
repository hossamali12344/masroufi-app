package com.masroufi.app.data.local

import androidx.room.*
import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.data.model.TransactionEntity
import com.masroufi.app.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isReviewedByUser = 0 AND necessity = 'PENDING_REVIEW' ORDER BY timestamp DESC")
    fun getPendingQuestions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsForPeriod(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET necessity = :necessity, isReviewedByUser = 1 WHERE id = :id")
    suspend fun updateNecessity(id: Long, necessity: ExpenseNecessity)

    @Query("UPDATE transactions SET receiptUri = :receiptUri, necessity = :necessity, userNotes = :notes WHERE id = :id")
    suspend fun attachReceiptAndClassification(id: Long, receiptUri: String, necessity: ExpenseNecessity, notes: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}
