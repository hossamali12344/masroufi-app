package com.masroufi.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * نوع المعاملة البنكية
 */
enum class TransactionType(val arabicName: String) {
    INCOMING_TRANSFER("حوالة محلية واردة"),
    OUTGOING_TRANSFER("حوالة محلية صادرة"),
    PURCHASE_POS("شراء نقاط بيع (POS)"),
    PURCHASE_ONLINE("شراء عبر الإنترنت"),
    ATM_WITHDRAWAL("سحب نقدي"),
    UNKNOWN("عملية بنكية")
}

/**
 * نوع البطاقة أو وسيلة الدفع المستخرجة من الرسالة
 */
enum class CardType(val arabicName: String) {
    MADA("بطاقة مدى"),
    VISA("فيزا (Visa)"),
    MASTERCARD("ماستركارد (Mastercard)"),
    CREDIT_CARD("بطاقة ائتمانية"),
    DEBIT_CARD("بطاقة حسم مباشر"),
    PREPAID("بطاقة مسبقة الدفع"),
    BANK_ACCOUNT("حساب بنكي"),
    UNKNOWN("وسيلة غير محددة")
}

/**
 * تصنيف المشتريات والمصروفات لتحديد الهدر
 */
enum class ExpenseNecessity(val arabicName: String) {
    ESSENTIAL("ضروري وأساسي"),       // طعام أساسي، فواتير، إيجار، صحة، وقود
    NON_ESSENTIAL("هدر وكماليات"),    // كافيهات، تسوق ترفيهي، مطاعم كمالية، اشتراكات غير مستغلة
    NEUTRAL("محايد / تحويل"),         // تحويل لحساب آخر أو استثمار
    PENDING_REVIEW("بانتظار التأكيد") // يسأل التطبيق المستخدم مباشرة عنها
}

/**
 * الكيان الرئيسي للمعاملة المالية
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val currency: String = "ر.س",
    val type: TransactionType,
    val cardType: CardType,
    val cardLastDigits: String? = null,
    val merchantOrParty: String, // اسم التاجر، أو الطرف المحول منه/إليه
    val bankSender: String,      // اسم البنك المستلم منه الرسالة (AlRajhi, SNB, CIB, إلخ)
    val originalSmsBody: String,
    val timestamp: Long = System.currentTimeMillis(),
    val necessity: ExpenseNecessity = ExpenseNecessity.PENDING_REVIEW,
    val userNotes: String? = null,
    val receiptUri: String? = null,
    val isReviewedByUser: Boolean = false,
    val category: String = "عام"
)

/**
 * عنصر فردي داخل الفاتورة بعد تحليلها
 */
data class ReceiptItem(
    val name: String,
    val price: Double,
    val isEssential: Boolean,
    val justification: String
)

/**
 * نتيجة تحليل الفاتورة عبر الذكاء الاصطناعي
 */
data class ReceiptAnalysisResult(
    val merchantName: String?,
    val totalAmount: Double,
    val items: List<ReceiptItem>,
    val essentialAmount: Double,
    val nonEssentialAmount: Double,
    val wastePercentage: Int,
    val aiInsight: String
)

/**
 * تقرير ومؤشر الهدر الشهري
 */
data class MonthlyWasteSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val essentialExpenses: Double,
    val wasteExpenses: Double,
    val wastePercentage: Double,
    val potentialMonthlySavings: Double,
    val pendingQuestionsCount: Int
)
