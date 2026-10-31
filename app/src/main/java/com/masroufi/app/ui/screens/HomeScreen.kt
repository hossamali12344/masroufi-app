package com.masroufi.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.data.model.TransactionType
import com.masroufi.app.ui.components.*
import com.masroufi.app.ui.theme.DarkGreen
import com.masroufi.app.ui.theme.PrimaryGreen
import com.masroufi.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()
    val pendingQuestions by viewModel.pendingQuestions.collectAsState()
    val receiptResult by viewModel.receiptAnalysisResult.collectAsState()
    val isAnalyzingReceipt by viewModel.isAnalyzingReceipt.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()

    var showReceiptDialog by remember { mutableStateOf(false) }
    var showSmsDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf(
        "الكل",
        "حوالات واردة 📥",
        "حوالات صادرة 📤",
        "نقاط بيع 🛒",
        "إنترنت 🌐",
        "هدر فقط 🔴"
    )

    // تصفية المعاملات حسب الفلتر المختار
    val filteredTransactions = remember(transactions, activeFilter) {
        when (activeFilter) {
            "حوالات واردة 📥" -> transactions.filter { it.type == TransactionType.INCOMING_TRANSFER }
            "حوالات صادرة 📤" -> transactions.filter { it.type == TransactionType.OUTGOING_TRANSFER }
            "نقاط بيع 🛒" -> transactions.filter { it.type == TransactionType.PURCHASE_POS }
            "إنترنت 🌐" -> transactions.filter { it.type == TransactionType.PURCHASE_ONLINE }
            "هدر فقط 🔴" -> transactions.filter { it.necessity == ExpenseNecessity.NON_ESSENTIAL }
            else -> transactions
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "مصروفي الذكي",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تتبع الدخل والمشتريات وكشف الهدر",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkGreen,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    actions = {
                        IconButton(onClick = { showSmsDialog = true }) {
                            Icon(imageVector = Icons.Default.Message, contentDescription = "محاكاة رسائل البنك")
                        }
                        IconButton(onClick = { showReceiptDialog = true }) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "تصوير فاتورة")
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showReceiptDialog = true },
                    containerColor = PrimaryGreen,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    text = { Text("فحص فاتورة وتقسيمها 🧾", fontWeight = FontWeight.Bold) }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // 1. مؤشر الهدر الشهري
                item {
                    MonthlyWasteGaugeCard(summary = monthlySummary)
                }

                // 2. بطاقة ملخص الدخل والمصروفات
                item {
                    IncomeExpenseSummaryRow(summary = monthlySummary)
                }

                // 3. سؤال المستشار المالي لتحديد الهدر (تفاعلي مباشر)
                item {
                    CoachInquiryCard(
                        questions = pendingQuestions,
                        onAnswer = { id, necessity ->
                            viewModel.answerQuestion(id, necessity)
                        }
                    )
                }

                // 4. أزرار تصفية المعاملات
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterOptions.forEach { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(filter, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // 5. عنوان قائمة المعاملات
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل المعاملات البنكية (${filteredTransactions.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحديث لحظي من الرسائل",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 6. قائمة المعاملات
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionCard(
                        transaction = tx,
                        onDelete = { viewModel.deleteTransaction(it) }
                    )
                }
            }
        }

        // نافذة تحليل الفاتورة
        if (showReceiptDialog) {
            ReceiptAnalysisDialog(
                result = receiptResult,
                isLoading = isAnalyzingReceipt,
                onDismiss = {
                    showReceiptDialog = false
                    viewModel.clearReceiptAnalysis()
                },
                onStartScan = {
                    // توليد محاكاة التقاط صورة وتحليلها بالذكاء الاصطناعي
                    val dummyBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                    viewModel.processReceipt(dummyBitmap)
                }
            )
        }

        // نافذة محاكاة واختبار رسائل SMS
        if (showSmsDialog) {
            SmsSimulatorDialog(
                onDismiss = { showSmsDialog = false },
                onSimulate = { smsText ->
                    viewModel.simulateBankSms(smsText)
                }
            )
        }
    }
}
