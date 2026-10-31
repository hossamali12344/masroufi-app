package com.masroufi.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroufi.app.ai.WasteInquiryQuestion
import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.ui.theme.*

@Composable
fun CoachInquiryCard(
    questions: List<WasteInquiryQuestion>,
    onAnswer: (transactionId: Long, necessity: ExpenseNecessity) -> Unit
) {
    if (questions.isEmpty()) return

    val currentQuestion = questions.first()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WarningOrangeLight),
        border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "سؤال المستشار المالي: تحديد الهدر 🎯",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8A3B00)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // تفاصيل المعاملة محل السؤال
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentQuestion.merchantOrParty,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = currentQuestion.cardDetails,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = currentQuestion.amountText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = currentQuestion.questionText,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Color(0xFF4A2600)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // أزرار الإجابة المباشرة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onAnswer(currentQuestion.transactionId, ExpenseNecessity.ESSENTIAL) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                ) {
                    Text(
                        text = "ضروري وأساسي 🟢",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { onAnswer(currentQuestion.transactionId, ExpenseNecessity.NON_ESSENTIAL) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(
                        text = "هدر وكماليات 🔴",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
