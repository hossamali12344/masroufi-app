package com.masroufi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroufi.app.data.model.MonthlyWasteSummary
import com.masroufi.app.ui.theme.*

@Composable
fun MonthlyWasteGaugeCard(summary: MonthlyWasteSummary) {
    val wastePct = summary.wastePercentage.coerceIn(0.0, 100.0).toInt()
    val gaugeColor = when {
        wastePct > 35 -> ExpenseRed
        wastePct in 15..35 -> WarningOrange
        else -> IncomeGreen
    }
    val statusText = when {
        wastePct > 35 -> "هدر مالي مرتفع ⚠️"
        wastePct in 15..35 -> "هدر معتدل ⚖️"
        else -> "استهلاك مثالي ومضبوط 🌟"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مؤشر الهدر المالي الشهري",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = gaugeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = gaugeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // شريط التقدم المرئي لنسبة الهدر
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wastePct%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = gaugeColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    LinearProgressIndicator(
                        progress = { (wastePct / 100f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = gaugeColor,
                        trackColor = Color(0xFFEEEEEE),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "نسبة الكماليات غير الضرورية من إجمالي مصاريفك",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "المصروف الضروري", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        text = "${String.format("%.1f", summary.essentialExpenses)} ر.س",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "مبلغ يمكن توفيره (الهدر)", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        text = "${String.format("%.1f", summary.potentialMonthlySavings)} ر.س",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseSummaryRow(summary: MonthlyWasteSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // بطاقة الدخل والحوالات الواردة
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = LightGreen)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "الدخل والوارد 📥",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "+${String.format("%.0f", summary.totalIncome)} ر.س",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen
                )
            }
        }

        // بطاقة المصروفات والمشتريات
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WasteRedLight)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "المشتريات والصادر 🛍️",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = WasteRedDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "-${String.format("%.0f", summary.totalExpense)} ر.س",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WasteRedDark
                )
            }
        }
    }
}
