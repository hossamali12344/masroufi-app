package com.masroufi.app.parser

import com.masroufi.app.data.model.CardType
import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.data.model.TransactionEntity
import com.masroufi.app.data.model.TransactionType
import java.util.regex.Pattern

/**
 * محرك ذكي متخصص في استخراج تفاصيل المعاملات البنكية من نصوص رسائل SMS
 * يدعم بنوك المملكة العربية السعودية ومصر والإمارات والخليج العربي
 */
object BankSmsParser {

    // قائمة الأسماء المعروفة لمقدمي الخدمات الأساسية (لتصنيف الضرورة تلقائياً)
    private val ESSENTIAL_KEYWORDS = listOf(
        "سوبرماركت", "بنده", "تموينات", "بقالة", "لولو", "كارفور", "العثيم", "الدانوب",
        "صيدلية", "الدواء", "النهدي", "مستشفى", "طبي", "مستوصف",
        "محطة", "بنزين", "وقود", "أدنوك", "نفط", "الدريس", "ساسكو",
        "كهرباء", "مياه", "فاتورة", "اتصالات", "stc", "mobily", "zain", "we", "vodafone",
        "grocery", "pharmacy", "fuel", "gas", "hospital", "supermarket"
    )

    // قائمة الكلمات الدالة على الكماليات والهدر المحتمل
    private val DISCRETIONARY_KEYWORDS = listOf(
        "كافيه", "قهوة", "مقهى", "كوفي", "starbucks", "barns", "cafe", "coffee", "roastery",
        "مطعم", "برجر", "شاورما", "حلويات", "شوكولاته", "restaurant", "burger", "pizza", "sweets",
        "زارا", "zara", "noon", "amazon", "نون", "أمازون", "shein", "شي ان",
        "سينما", "cinema", "vox", "muvi", "ألعاب", "ترفيه", "gaming", "steam", "playstation",
        "عطور", "boutique", "سياحة", "منتجع", "lounge", "spa", "صالون"
    )

    fun parse(smsBody: String, sender: String = "بنك"): TransactionEntity? {
        val cleanBody = smsBody.replace("\r", " ").trim()

        // 1. استخراج نوع المعاملة
        val type = extractTransactionType(cleanBody)
        if (type == TransactionType.UNKNOWN && !isFinancialMessage(cleanBody)) {
            return null // ليست رسالة مالية
        }

        // 2. استخراج المبلغ والعملة
        val (amount, currency) = extractAmountAndCurrency(cleanBody) ?: return null

        // 3. استخراج نوع البطاقة والوسيلة
        val cardType = extractCardType(cleanBody, type)

        // 4. استخراج آخر 4 أرقام من البطاقة أو الحساب
        val cardDigits = extractCardOrAccountDigits(cleanBody)

        // 5. استخراج اسم التاجر أو الطرف الآخر (المستفيد / الراسل)
        val party = extractMerchantOrParty(cleanBody, type)

        // 6. تقدير أولي لضرورة المصروف (ضروري أم كماليات/هدر أم انتظار تأكيد)
        val initialNecessity = evaluateInitialNecessity(type, party, cleanBody)

        return TransactionEntity(
            amount = amount,
            currency = currency,
            type = type,
            cardType = cardType,
            cardLastDigits = cardDigits,
            merchantOrParty = party,
            bankSender = sender,
            originalSmsBody = cleanBody,
            necessity = initialNecessity,
            category = categorizeMerchant(party, type)
        )
    }

    private fun isFinancialMessage(body: String): Boolean {
        val lower = body.lowercase()
        return lower.contains("شراء") || lower.contains("حوالة") || lower.contains("مبلغ") ||
                lower.contains("سداد") || lower.contains("خصم") || lower.contains("إيداع") ||
                lower.contains("purchase") || lower.contains("transfer") || lower.contains("debited") ||
                lower.contains("credited") || lower.contains("pos")
    }

    private fun extractTransactionType(body: String): TransactionType {
        val text = body.lowercase()

        // حوالة محلية واردة
        if (text.contains("حوالة واردة") || text.contains("حوالة محلية واردة") ||
            text.contains("تم استلام حوالة") || text.contains("إيداع") ||
            text.contains("تم إضافة مبلغ") || text.contains("تحويل وارد") ||
            text.contains("credited") || text.contains("inward transfer") || text.contains("salary")
        ) {
            return TransactionType.INCOMING_TRANSFER
        }

        // حوالة محلية صادرة
        if (text.contains("حوالة صادرة") || text.contains("حوالة محلية صادرة") ||
            text.contains("تم تحويل") || text.contains("تحويل سريع") ||
            text.contains("سريع محلي") || text.contains("outward transfer") ||
            text.contains("transferred to")
        ) {
            return TransactionType.OUTGOING_TRANSFER
        }

        // شراء عبر الإنترنت
        if (text.contains("عبر الإنترنت") || text.contains("إنترنت") ||
            text.contains("online") || text.contains("e-commerce") ||
            text.contains("متجر إلكتروني") || text.contains("apple pay online")
        ) {
            return TransactionType.PURCHASE_ONLINE
        }

        // شراء نقاط بيع POS
        if (text.contains("نقاط البيع") || text.contains("pos") ||
            text.contains("شراء") || text.contains("purchase") ||
            text.contains("عملية شراء") || text.contains("مدى pay") || text.contains("apple pay")
        ) {
            return TransactionType.PURCHASE_POS
        }

        // سحب نقدي
        if (text.contains("سحب نقدي") || text.contains("صراف") || text.contains("atm withdrawal")) {
            return TransactionType.ATM_WITHDRAWAL
        }

        return TransactionType.UNKNOWN
    }

    private fun extractAmountAndCurrency(body: String): Pair<Double, String>? {
        // نمط للبحث عن المبالغ مثل 1,250.50 ر.س أو SAR 450 أو 300.00 ج.م
        val pattern = Pattern.compile(
            """(?:بمبلغ|بقيمة|مبلغ|بـ|amount|purchase of|of)\s*[:]?\s*(?:SAR|EGP|AED|USD|ر\.س|ريال|ج\.م|د\.إ|\$)?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(SAR|EGP|AED|USD|ر\.س|ريال|ج\.م|د\.إ|\$|ريالاً)?""",
            Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        )

        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            val amountStr = matcher.group(1)?.replace(",", "") ?: return null
            val rawCurrency = matcher.group(2) ?: "ر.س"
            val currency = normalizeCurrency(rawCurrency, body)
            val amount = amountStr.toDoubleOrNull() ?: return null
            return Pair(amount, currency)
        }

        // نمط بديل إذا لم تكن كلمة (بمبلغ) موجودة
        val altPattern = Pattern.compile(
            """([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(ر\.س|ريال|SAR|ج\.م|EGP|AED|د\.إ|USD|\$)""",
            Pattern.CASE_INSENSITIVE
        )
        val altMatcher = altPattern.matcher(body)
        if (altMatcher.find()) {
            val amountStr = altMatcher.group(1)?.replace(",", "") ?: return null
            val rawCurrency = altMatcher.group(2) ?: "ر.س"
            val currency = normalizeCurrency(rawCurrency, body)
            val amount = amountStr.toDoubleOrNull() ?: return null
            return Pair(amount, currency)
        }

        return null
    }

    private fun normalizeCurrency(curr: String, body: String): String {
        val c = curr.trim()
        if (c.contains("ج.م") || c.contains("EGP") || body.contains("ج.م") || body.contains("بنك مصر") || body.contains("الأهلي المصري")) {
            return "ج.م"
        }
        if (c.contains("د.إ") || c.contains("AED")) {
            return "د.إ"
        }
        if (c.contains("$") || c.contains("USD")) {
            return "USD"
        }
        return "ر.س"
    }

    private fun extractCardType(body: String, transactionType: TransactionType): CardType {
        val lower = body.lowercase()
        return when {
            lower.contains("مدى") || lower.contains("mada") -> CardType.MADA
            lower.contains("فيزا") || lower.contains("visa") -> CardType.VISA
            lower.contains("ماستركارد") || lower.contains("mastercard") -> CardType.MASTERCARD
            lower.contains("ائتمانية") || lower.contains("الائتمانية") || lower.contains("credit") -> CardType.CREDIT_CARD
            lower.contains("مسبقة الدفع") || lower.contains("prepaid") -> CardType.PREPAID
            lower.contains("حسم") || lower.contains("خصم فوري") || lower.contains("مدين") || lower.contains("debit") -> CardType.DEBIT_CARD
            transactionType == TransactionType.INCOMING_TRANSFER || transactionType == TransactionType.OUTGOING_TRANSFER -> CardType.BANK_ACCOUNT
            else -> CardType.UNKNOWN
        }
    }

    private fun extractCardOrAccountDigits(body: String): String? {
        val patterns = listOf(
            Pattern.compile("""(?:بطاقة|حساب|حسابك|حسابكم|card|a/c|ending in|تنتهي بـ|المطلوبة|منتهية بـ)\s*[:]?\s*(?:\*|#|x)*([0-9]{4})""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:\*{2,4}|#{2,4}|X{2,4})([0-9]{4})""")
        )

        for (pat in patterns) {
            val matcher = pat.matcher(body)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    private fun extractMerchantOrParty(body: String, type: TransactionType): String {
        val lower = body.lowercase()

        // للمشتريات: لدى [اسم التاجر]
        if (type == TransactionType.PURCHASE_POS || type == TransactionType.PURCHASE_ONLINE) {
            val merchantPattern = Pattern.compile(
                """(?:لدى|عند|في متجر|at)\s*[:]?\s*([^\n,،؛]+?)(?:\s*(?:بواسطة|عبر|بتاريخ|في|رصيدك|using|with|on|\.|$))""",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = merchantPattern.matcher(body)
            if (matcher.find()) {
                val found = matcher.group(1)?.trim()
                if (!found.isNullOrBlank() && found.length < 50) {
                    return found
                }
            }
        }

        // للحوالة الواردة: من [اسم الراسل]
        if (type == TransactionType.INCOMING_TRANSFER) {
            val fromPattern = Pattern.compile(
                """(?:من|from)\s*[:]?\s*([^\n,،؛]+?)(?:\s*(?:إلى|لحسابك|عبر|بتاريخ|رصيدك|to|\.|$))""",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = fromPattern.matcher(body)
            if (matcher.find()) {
                val found = matcher.group(1)?.trim()
                if (!found.isNullOrBlank() && found.length < 50) {
                    return found
                }
            }
            return "مرسل محلي"
        }

        // للحوالة الصادرة: إلى [المستفيد]
        if (type == TransactionType.OUTGOING_TRANSFER) {
            val toPattern = Pattern.compile(
                """(?:إلى|لصالح|to)\s*[:]?\s*([^\n,،؛]+?)(?:\s*(?:من|عبر|بتاريخ|رصيدك|\.|$))""",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = toPattern.matcher(body)
            if (matcher.find()) {
                val found = matcher.group(1)?.trim()
                if (!found.isNullOrBlank() && found.length < 50) {
                    return found
                }
            }
            return "مستفيد محلي"
        }

        return "متجر غير محدد"
    }

    private fun evaluateInitialNecessity(
        type: TransactionType,
        party: String,
        body: String
    ): ExpenseNecessity {
        if (type == TransactionType.INCOMING_TRANSFER) {
            return ExpenseNecessity.NEUTRAL
        }

        val combined = "$party $body".lowercase()

        // فحص الكلمات الأساسية
        for (keyword in ESSENTIAL_KEYWORDS) {
            if (combined.contains(keyword.lowercase())) {
                return ExpenseNecessity.ESSENTIAL
            }
        }

        // فحص الكماليات والهدر
        for (keyword in DISCRETIONARY_KEYWORDS) {
            if (combined.contains(keyword.lowercase())) {
                return ExpenseNecessity.NON_ESSENTIAL
            }
        }

        // إذا كانت المعاملة غير واضحة (مثل متجر غير معروف أو تحويل صادرة)، نجعلها بانتظار سؤال المستخدم
        return ExpenseNecessity.PENDING_REVIEW
    }

    private fun categorizeMerchant(party: String, type: TransactionType): String {
        val lower = party.lowercase()
        return when {
            type == TransactionType.INCOMING_TRANSFER -> "دخل وحوالات واردة"
            type == TransactionType.OUTGOING_TRANSFER -> "حوالات وتدوير أموال"
            lower.contains("كافيه") || lower.contains("قهوة") || lower.contains("cafe") || lower.contains("coffee") -> "كافيهات ومشروبات"
            lower.contains("مطعم") || lower.contains("برجر") || lower.contains("restaurant") -> "مطاعم ووجبات"
            lower.contains("بنده") || lower.contains("سوبرماركت") || lower.contains("بقالة") || lower.contains("العثيم") -> "بقالة ومستلزمات منزل"
            lower.contains("صيدلية") || lower.contains("مستشفى") -> "صحة ورعاية"
            lower.contains("بنزين") || lower.contains("محطة") || lower.contains("وقود") -> "نقل ومواصلات"
            lower.contains("نون") || lower.contains("أمازون") || lower.contains("amazon") || lower.contains("noon") -> "تسوق إلكتروني"
            else -> "مشتريات عامة"
        }
    }
}
