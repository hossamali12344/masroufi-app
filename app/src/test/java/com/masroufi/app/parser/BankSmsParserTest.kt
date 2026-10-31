package com.masroufi.app.parser

import com.masroufi.app.data.model.CardType
import com.masroufi.app.data.model.ExpenseNecessity
import com.masroufi.app.data.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class BankSmsParserTest {

    @Test
    fun testIncomingLocalTransfer() {
        val sms = "حوالة محلية واردة بمبلغ 5,000.00 ر.س من فهد أحمد إلى حسابك *1234 رصيدك المتاح 8,500.00 ر.س"
        val parsed = BankSmsParser.parse(sms, "AlRajhi")

        assertNotNull(parsed)
        assertEquals(TransactionType.INCOMING_TRANSFER, parsed?.type)
        assertEquals(5000.0, parsed?.amount ?: 0.0, 0.01)
        assertEquals("1234", parsed?.cardLastDigits)
        assertEquals(CardType.BANK_ACCOUNT, parsed?.cardType)
        assertTrue(parsed?.merchantOrParty?.contains("فهد") == true)
        assertEquals(ExpenseNecessity.NEUTRAL, parsed?.necessity)
    }

    @Test
    fun testOutgoingLocalTransfer() {
        val sms = "حوالة محلية صادرة بمبلغ 1,200.00 ر.س إلى شركة الصيانة من حسابك *5678"
        val parsed = BankSmsParser.parse(sms, "SNB")

        assertNotNull(parsed)
        assertEquals(TransactionType.OUTGOING_TRANSFER, parsed?.type)
        assertEquals(1200.0, parsed?.amount ?: 0.0, 0.01)
        assertEquals("5678", parsed?.cardLastDigits)
        assertTrue(parsed?.merchantOrParty?.contains("شركة الصيانة") == true)
    }

    @Test
    fun testPosPurchaseWithMada() {
        val sms = "شراء عبر نقاط البيع (POS) بقيمة 65.50 ر.س لدى BARNS COFFEE بواسطة بطاقة مدى *4432"
        val parsed = BankSmsParser.parse(sms, "Alinma")

        assertNotNull(parsed)
        assertEquals(TransactionType.PURCHASE_POS, parsed?.type)
        assertEquals(65.50, parsed?.amount ?: 0.0, 0.01)
        assertEquals(CardType.MADA, parsed?.cardType)
        assertEquals("4432", parsed?.cardLastDigits)
        assertEquals(ExpenseNecessity.NON_ESSENTIAL, parsed?.necessity) // مقهى -> صُنف كماليات
    }

    @Test
    fun testOnlinePurchaseWithVisa() {
        val sms = "شراء عبر الإنترنت بقيمة 299.00 ر.س لدى AMAZON بواسطة بطاقة فيزا *9811"
        val parsed = BankSmsParser.parse(sms, "RiyadBank")

        assertNotNull(parsed)
        assertEquals(TransactionType.PURCHASE_ONLINE, parsed?.type)
        assertEquals(299.00, parsed?.amount ?: 0.0, 0.01)
        assertEquals(CardType.VISA, parsed?.cardType)
        assertEquals("9811", parsed?.cardLastDigits)
    }

    @Test
    fun testEssentialSupermarketPurchase() {
        val sms = "شراء بقيمة 420.00 ر.س لدى سوبرماركت بنده بواسطة بطاقة مدى *8899"
        val parsed = BankSmsParser.parse(sms, "AlRajhi")

        assertNotNull(parsed)
        assertEquals(TransactionType.PURCHASE_POS, parsed?.type)
        assertEquals(ExpenseNecessity.ESSENTIAL, parsed?.necessity) // سوبرماركت بنده -> صُنف كضروري أساسي
    }
}
