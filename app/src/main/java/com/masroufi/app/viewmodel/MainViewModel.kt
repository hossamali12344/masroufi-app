package com.masroufi.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masroufi.app.ai.FinancialCoachAdvisor
import com.masroufi.app.ai.ReceiptAnalyzerService
import com.masroufi.app.ai.WasteInquiryQuestion
import com.masroufi.app.data.local.AppDatabase
import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.data.model.MonthlyWasteSummary
import com.masroufi.app.data.model.ReceiptAnalysisResult
import com.masroufi.app.data.model.TransactionEntity
import com.masroufi.app.data.repository.TransactionRepository
import com.masroufi.app.parser.BankSmsParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    private val receiptAnalyzer: ReceiptAnalyzerService

    val transactions: StateFlow<List<TransactionEntity>>
    val monthlySummary: StateFlow<MonthlyWasteSummary>

    private val _pendingQuestions = MutableStateFlow<List<WasteInquiryQuestion>>(emptyList())
    val pendingQuestions: StateFlow<List<WasteInquiryQuestion>> = _pendingQuestions.asStateFlow()

    private val _receiptAnalysisResult = MutableStateFlow<ReceiptAnalysisResult?>(null)
    val receiptAnalysisResult: StateFlow<ReceiptAnalysisResult?> = _receiptAnalysisResult.asStateFlow()

    private val _isAnalyzingReceipt = MutableStateFlow(false)
    val isAnalyzingReceipt: StateFlow<Boolean> = _isAnalyzingReceipt.asStateFlow()

    private val _activeFilter = MutableStateFlow("الكل")
    val activeFilter: StateFlow<String> = _activeFilter.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TransactionRepository(db.transactionDao())
        receiptAnalyzer = ReceiptAnalyzerService(application)

        transactions = repository.allTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        monthlySummary = repository.getCurrentMonthWasteSummary().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MonthlyWasteSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0)
        )

        // تحديث أسئلة المساعد المالي تلقائياً كلما تغيرت المعاملات
        viewModelScope.launch {
            transactions.collect { list ->
                if (list.isEmpty()) {
                    // إدراج أمثلة واقعية أولية حتى يرى المستخدم مباشرة كيفية تفاعل التطبيق
                    populateDemoTransactionsIfEmpty()
                } else {
                    _pendingQuestions.value = FinancialCoachAdvisor.generateInquiries(list)
                }
            }
        }
    }

    fun setFilter(filter: String) {
        _activeFilter.value = filter
    }

    fun answerQuestion(transactionId: Long, necessity: ExpenseNecessity) {
        viewModelScope.launch {
            repository.updateNecessity(transactionId, necessity)
        }
    }

    fun processReceipt(bitmap: Bitmap, apiKey: String? = null) {
        viewModelScope.launch {
            _isAnalyzingReceipt.value = true
            val result = receiptAnalyzer.analyzeReceipt(bitmap, apiKey)
            _receiptAnalysisResult.value = result
            _isAnalyzingReceipt.value = false
        }
    }

    fun clearReceiptAnalysis() {
        _receiptAnalysisResult.value = null
    }

    /**
     * محاكاة وصول رسالة بنكية لاختبار التعرف عليها لحظياً
     */
    fun simulateBankSms(smsBody: String, sender: String = "AlRajhiBank") {
        viewModelScope.launch {
            val parsed = BankSmsParser.parse(smsBody, sender)
            if (parsed != null) {
                repository.insertTransaction(parsed)
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    private suspend fun populateDemoTransactionsIfEmpty() {
        val demoSmsList = listOf(
            "حوالة محلية واردة بمبلغ 14,500.00 ر.س من شركة التقنية إلى حسابك *4512 رصيدك المتاح 18,200.00 ر.س",
            "حوالة محلية صادرة بمبلغ 2,500.00 ر.س إلى أحمد العتيبي من حسابك *4512 رسوم التحويل 0.00 ر.س",
            "شراء عبر نقاط البيع (POS) بقيمة 34.50 ر.س لدى BARNS COFFEE بواسطة بطاقة مدى *8821 في 28/10",
            "شراء عبر الإنترنت بقيمة 480.00 ر.س لدى NOON بواسطة بطاقة فيزا الائتمانية *7734 في 29/10",
            "شراء عبر نقاط البيع بقيمة 312.00 ر.س لدى سوبرماركت بنده بواسطة بطاقة مدى *8821 في 30/10",
            "عملية شراء بقيمة 120.00 ر.س لدى STARBUCKS بواسطة بطاقة ماستركارد *1144"
        )

        for (sms in demoSmsList) {
            val parsed = BankSmsParser.parse(sms, "البنك التجاري")
            if (parsed != null) {
                repository.insertTransaction(parsed)
            }
        }
    }
}
