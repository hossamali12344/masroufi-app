package com.masroufi.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.masroufi.app.data.model.CardType
import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.data.model.TransactionEntity
import com.masroufi.app.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = runCatching {
        TransactionType.valueOf(value)
    }.getOrDefault(TransactionType.UNKNOWN)

    @TypeConverter
    fun fromCardType(value: CardType): String = value.name

    @TypeConverter
    fun toCardType(value: String): CardType = runCatching {
        CardType.valueOf(value)
    }.getOrDefault(CardType.UNKNOWN)

    @TypeConverter
    fun fromExpenseNecessity(value: ExpenseNecessity): String = value.name

    @TypeConverter
    fun toExpenseNecessity(value: String): ExpenseNecessity = runCatching {
        ExpenseNecessity.valueOf(value)
    }.getOrDefault(ExpenseNecessity.PENDING_REVIEW)
}

@Database(entities = [TransactionEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "masroufi_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
