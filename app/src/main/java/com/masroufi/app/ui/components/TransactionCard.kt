package com.masroufi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masroufi.app.data.model.CardType
import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.data.model.TransactionEntity
import com.masroufi.app.data.model.TransactionType
import com.masroufi.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    onDelete: (Long) -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOMING_TRANSFER
    val formattedDate = SimpleDateFormat("dd/MM | hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.8.dp, Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // أيقونة نوع المعاملة
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(getTransactionIconBg(transaction.type)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getTransactionIcon(transaction.type),
                        contentDescription = null,
                        tint = getTransactionIconTint(transaction.type),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // اسم التاجر ونوع المعاملة
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchantOrParty,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.type.arabicName} • $formattedDate",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                // المبلغ
                Column(horizontalAlignment = Alignment.End) {
                    val prefix = if (isIncome) "+" else "-"
                    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
                    Text(
                        text = "$prefix${transaction.amount} ${transaction.currency}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = amountColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color(0xFFF6F6F6))
            Spacer(modifier = Modifier.height(8.dp))

            // الشارات السفلية (نوع البطاقة + تصنيف الضرورة)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // شارة البطاقة
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getCardBadgeColor(transaction.cardType).copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${transaction.cardType.arabicName} ${transaction.cardLastDigits?.let { "*$it" } ?: ""}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = getCardBadgeColor(transaction.cardType)
                        )
                    }
                }

                // شارة تصنيف الهدر / الضرورة
                if (!isIncome) {
                    val (badgeText, badgeBg, badgeTextColor) = when (transaction.necessity) {
                        ExpenseNecessity.ESSENTIAL -> Triple("ضروري وأساسي 🟢", EssentialGreenLight, EssentialGreenDark)
                        ExpenseNecessity.NON_ESSENTIAL -> Triple("هدر وكماليات 🔴", WasteRedLight, WasteRedDark)
                        ExpenseNecessity.PENDING_REVIEW -> Triple("بانتظار التأكيد 🟡", WarningOrangeLight, WarningOrange)
                        ExpenseNecessity.NEUTRAL -> Triple("تحويل محايد ⚪", Color(0xFFF0F0F0), TextSecondary)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }
            }
        }
    }
}

private fun getTransactionIcon(type: TransactionType): ImageVector = when (type) {
    TransactionType.INCOMING_TRANSFER -> Icons.Default.ArrowDownward
    TransactionType.OUTGOING_TRANSFER -> Icons.Default.ArrowUpward
    TransactionType.PURCHASE_POS -> Icons.Default.ShoppingCart
    TransactionType.PURCHASE_ONLINE -> Icons.Default.Public
    TransactionType.ATM_WITHDRAWAL -> Icons.Default.AccountBalanceWallet
    TransactionType.UNKNOWN -> Icons.Default.CreditCard
}

private fun getTransactionIconBg(type: TransactionType): Color = when (type) {
    TransactionType.INCOMING_TRANSFER -> LightGreen
    TransactionType.OUTGOING_TRANSFER -> Color(0xFFE8EAF6)
    TransactionType.PURCHASE_POS -> Color(0xFFFFF3E0)
    TransactionType.PURCHASE_ONLINE -> Color(0xFFE1F5FE)
    TransactionType.ATM_WITHDRAWAL -> Color(0xFFEDE7F6)
    TransactionType.UNKNOWN -> Color(0xFFF5F5F5)
}

private fun getTransactionIconTint(type: TransactionType): Color = when (type) {
    TransactionType.INCOMING_TRANSFER -> DarkGreen
    TransactionType.OUTGOING_TRANSFER -> Color(0xFF3949AB)
    TransactionType.PURCHASE_POS -> WarningOrange
    TransactionType.PURCHASE_ONLINE -> Color(0xFF0288D1)
    TransactionType.ATM_WITHDRAWAL -> Color(0xFF5E35B1)
    TransactionType.UNKNOWN -> TextSecondary
}

private fun getCardBadgeColor(cardType: CardType): Color = when (cardType) {
    CardType.MADA -> MadaColor
    CardType.VISA -> VisaColor
    CardType.MASTERCARD -> MastercardColor
    CardType.CREDIT_CARD -> Color(0xFFD81B60)
    CardType.DEBIT_CARD -> Color(0xFF00897B)
    CardType.PREPAID -> Color(0xFFFB8C00)
    CardType.BANK_ACCOUNT -> Color(0xFF546E7A)
    CardType.UNKNOWN -> TextSecondary
}
