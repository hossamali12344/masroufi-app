package com.masroufi.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.masroufi.app.ui.theme.DarkGreen
import com.masroufi.app.ui.theme.PrimaryGreen

@Composable
fun SmsSimulatorDialog(
    onDismiss: () -> Unit,
    onSimulate: (String) -> Unit
) {
    var customSms by remember { mutableStateOf("") }

    val presetMessages = listOf(
        "حوالة محلية واردة بمبلغ 6,500.00 ر.س من خالد العبدالله إلى حسابك *1234 رصيدك المتاح 12,000.00 ر.س",
        "حوالة محلية صادرة بمبلغ 850.00 ر.س إلى شركة الصيانة من حسابك *1234",
        "شراء عبر نقاط البيع (POS) بقيمة 68.00 ر.س لدى STARBUCKS بواسطة بطاقة مدى *5541",
        "شراء عبر الإنترنت بقيمة 249.00 ر.س لدى AMAZON بواسطة بطاقة فيزا *9912",
        "عملية شراء بقيمة 430.00 ر.س لدى ZARA بواسطة بطاقتك الائتمانية ماستركارد *3321"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "محاكاة رسائل البنوك (SMS) 📱",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "اختر رسالة بنكية لاختبار التعرف الفوري على نوع الحوالة والبطاقة والمبلغ:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                presetMessages.forEach { msg ->
                    OutlinedButton(
                        onClick = {
                            onSimulate(msg)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = msg.take(45) + "...",
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = customSms,
                    onValueChange = { customSms = it },
                    label = { Text("أو الصق نص رسالة بنكك هنا") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (customSms.isNotBlank()) {
                            onSimulate(customSms)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = customSms.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("اختبار الرسالة الآن", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
