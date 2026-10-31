package com.masroufi.app.ai

import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.data.model.TransactionEntity
import com.masroufi.app.data.model.TransactionType

data class WasteInquiryQuestion(
    val transactionId: Long,
    val title: String,
    val questionText: String,
    val merchantOrParty: String,
    val amountText: String,
    val cardDetails: String,
    val options: List<InquiryOption>
)

data class InquiryOption(
    val label: String,
    val necessity: ExpenseNecessity,
    val feedback: String
)

/**
 * المساعد المالي التفاعلي: يوجه أسئلة مباشرة للمستخدم بناءً على معاملاته
 * لتحديد هل الصرف كان استثماراً وضرورة أم هدر مالي يمكن تفاديه مستقبلاً
 */
object FinancialCoachAdvisor {

    fun generateInquiries(transactions: List<TransactionEntity>): List<WasteInquiryQuestion> {
        val questions = mutableListOf<WasteInquiryQuestion>()

        // 1. المعاملات التي بانتظار المراجعة أو ذات طابع محير
        val unclassified = transactions.filter {
            it.type != TransactionType.INCOMING_TRANSFER &&
            (!it.isReviewedByUser || it.necessity == ExpenseNecessity.PENDING_REVIEW)
        }.take(5)

        for (tx in unclassified) {
            val cardDesc = "${tx.cardType.arabicName} ${tx.cardLastDigits?.let { "تنتهي بـ $it" } ?: ""}"

            val (title, questionText) = when {
                tx.type == TransactionType.OUTGOING_TRANSFER -> Pair(
                    "حوالة مالية صادرة",
                    "أرسلت مبلغ ${tx.amount} ${tx.currency} إلى \"${tx.merchantOrParty}\". هل كانت هذه الحوالة لسداد التزام ضروري (فواتير/دين/إيجار) أم مصاريف ترفيهية وشخصية؟"
                )
                tx.type == TransactionType.PURCHASE_ONLINE -> Pair(
                    "تسوق عبر الإنترنت",
                    "رصدنا عملية شراء بـ ${tx.amount} ${tx.currency} لدى \"${tx.merchantOrParty}\" عبر $cardDesc. هل كانت السلعة ضرورية لحياتك اليومية أم شراء اندفاعي يعتبر هدراً؟"
                )
                tx.merchantOrParty.contains("كافيه") || tx.merchantOrParty.contains("قهوة") || tx.merchantOrParty.contains("كوفي") -> Pair(
                    "مصروف كافيهات ومشروبات",
                    "صرفت ${tx.amount} ${tx.currency} في \"${tx.merchantOrParty}\". هل ترى هذا الصرف ضرورياً لروتينك أم هدر يمكن توفيره بصنع القهوة منزلياً؟"
                )
                else -> Pair(
                    "مشتريات نقطة بيع",
                    "تم الشراء بمبلغ ${tx.amount} ${tx.currency} لدى \"${tx.merchantOrParty}\". كيف تصنف هذه المعاملة ضمن ميزانيتك؟"
                )
            }

            questions.add(
                WasteInquiryQuestion(
                    transactionId = tx.id,
                    title = title,
                    questionText = questionText,
                    merchantOrParty = tx.merchantOrParty,
                    amountText = "${tx.amount} ${tx.currency}",
                    cardDetails = cardDesc,
                    options = listOf(
                        InquiryOption(
                            label = "ضرورية وأساسية 🟢",
                            necessity = ExpenseNecessity.ESSENTIAL,
                            feedback = "تم تصنيفها كبند أساسي ومحسوب."
                        ),
                        InquiryOption(
                            label = "هدر وكماليات كان يمكن تجنبها 🔴",
                            necessity = ExpenseNecessity.NON_ESSENTIAL,
                            feedback = "أحسنت باعترافك! سنضيفها لمؤشر الهدر لضبط ميزانيتك الشهر القادم."
                        )
                    )
                )
            )
        }

        return questions
    }

    /**
     * نصائح مالية بناءً على نسبة الهدر الشهرية
     */
    fun getMonthlyAdvice(wastePercentage: Double, wasteAmount: Double, currency: String): String {
        return when {
            wastePercentage > 35 -> "⚠️ تنبيه مالي: نسبة الهدر في مشترياتك بلغت ${wastePercentage.toInt()}%. بإمكانك توفير ما لا يقل عن ${wasteAmount.toInt()} $currency شهرياً فقط بتقليل ارتياد المقاهي والمشتريات الإلكترونية الاندفاعية."
            wastePercentage in 15.0..35.0 -> "💡 وضعك المالي متوازن نسبياً: الهدر يمثل ${wastePercentage.toInt()}%. ركز على الاستفسارات المعلقة وتصوير فواتير السوبرماركت لفصل كماليات السناكات عن الأساسيات."
            else -> "🌟 ممتاز جداً! إنفاقك منضبط والكماليات أقل من 15%. واصل على هذا النهج الذكي في إدارة دخلك."
        }
    }
}
