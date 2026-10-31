package com.masroufi.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.masroufi.app.MainActivity
import com.masroufi.app.R
import com.masroufi.app.data.local.AppDatabase
import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.data.model.TransactionEntity
import com.masroufi.app.parser.BankSmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val fullBody = StringBuilder()
        var sender = "Bank"
        for (msg in messages) {
            sender = msg.originatingAddress ?: "Bank"
            fullBody.append(msg.messageBody)
        }

        val smsText = fullBody.toString()

        // تحليل الرسالة عبر المحرك الذكي
        val parsedTransaction = BankSmsParser.parse(smsText, sender) ?: return

        // حفظ المعاملة في قاعدة البيانات
        val db = AppDatabase.getDatabase(context)
        CoroutineScope(Dispatchers.IO).launch {
            val insertedId = db.transactionDao().insertTransaction(parsedTransaction)

            // إرسال إشعار تفاعلي للمستخدم إذا كانت المعاملة تتطلب تصنيف الهدر أو الاستفسار المباشر
            notifyUserAboutTransaction(context, parsedTransaction.copy(id = insertedId))
        }
    }

    private fun notifyUserAboutTransaction(context: Context, tx: TransactionEntity) {
        val channelId = "bank_transactions_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "المعاملات المالية والهدر",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات تتبع المصروفات واستفسارات الهدر المالي المباشرة"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TRANSACTION_ID", tx.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            tx.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${tx.type.arabicName} | ${tx.amount} ${tx.currency}"
        val content = if (tx.necessity == ExpenseNecessity.PENDING_REVIEW) {
            "تم الشراء لدى: ${tx.merchantOrParty} (${tx.cardType.arabicName}). اضغط لتحديد: هل هي ضرورية أم هدر؟ أو صوّر الفاتورة."
        } else {
            "تم رصد عملية لدى: ${tx.merchantOrParty} - تصنيف: ${tx.necessity.arabicName}"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(tx.id.toInt(), notification)
    }
}
