package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatUsageManager
import com.example.data.local.ScanUsageManager
import com.example.viewmodel.CurbViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UsageQuotaIntegrityTest {

    private lateinit var app: Application
    private lateinit var scanManager: ScanUsageManager
    private lateinit var chatManager: ChatUsageManager

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        scanManager = ScanUsageManager(app)
        chatManager = ChatUsageManager(app)
        scanManager.resetUsage()
        chatManager.resetUsage()
    }

    @Test
    fun `scan limit is exactly 10 per month for free users`() {
        repeat(9) {
            assertTrue(scanManager.canPerformScan(isPro = false))
            scanManager.consumeScan(isPro = false)
        }

        // 10th scan
        val info10 = scanManager.getUsageInfo(isPro = false)
        assertEquals(9, info10.scansUsed)
        assertEquals(1, info10.scansRemaining)
        assertFalse(info10.isLimitReached)
        assertTrue(scanManager.canPerformScan(isPro = false))

        scanManager.consumeScan(isPro = false)

        // 11th attempt - limit reached
        val info11 = scanManager.getUsageInfo(isPro = false)
        assertEquals(10, info11.scansUsed)
        assertEquals(0, info11.scansRemaining)
        assertTrue(info11.isLimitReached)
        assertFalse(scanManager.canPerformScan(isPro = false))
    }

    @Test
    fun `chat limit is exactly 15 per day for free users`() {
        repeat(14) {
            assertTrue(chatManager.canSendMessage(isPro = false))
            chatManager.incrementUsage(isPro = false)
        }

        // 15th question
        val info15 = chatManager.getUsageInfo(isPro = false)
        assertEquals(14, info15.messagesUsedToday)
        assertEquals(1, info15.messagesRemaining)
        assertFalse(info15.isLimitReached)
        assertTrue(chatManager.canSendMessage(isPro = false))

        chatManager.incrementUsage(isPro = false)

        // 16th attempt - limit reached
        val info16 = chatManager.getUsageInfo(isPro = false)
        assertEquals(15, info16.messagesUsedToday)
        assertEquals(0, info16.messagesRemaining)
        assertTrue(info16.isLimitReached)
        assertFalse(chatManager.canSendMessage(isPro = false))
    }

    @Test
    fun `scan and chat usage persist across app restarts`() {
        scanManager.consumeScan(isPro = false)
        scanManager.consumeScan(isPro = false)
        chatManager.incrementUsage(isPro = false)

        val newScanManager = ScanUsageManager(app)
        val newChatManager = ChatUsageManager(app)

        assertEquals(2, newScanManager.getUsageInfo(isPro = false).scansUsed)
        assertEquals(8, newScanManager.getUsageInfo(isPro = false).scansRemaining)
        assertEquals(1, newChatManager.getUsageInfo(isPro = false).messagesUsedToday)
        assertEquals(14, newChatManager.getUsageInfo(isPro = false).messagesRemaining)
    }

    @Test
    fun `Pro and Judge Demo Pro bypass scan and chat limits without corrupting free usage counters`() {
        scanManager.consumeScan(isPro = false)
        scanManager.consumeScan(isPro = false)
        chatManager.incrementUsage(isPro = false)

        // Pro check
        val proScanInfo = scanManager.getUsageInfo(isPro = true)
        val proChatInfo = chatManager.getUsageInfo(isPro = true)

        assertTrue(scanManager.canPerformScan(isPro = true))
        assertTrue(chatManager.canSendMessage(isPro = true))
        assertEquals("Unlimited AI scans", proScanInfo.displayText)
        assertEquals("Unlimited AI questions", proChatInfo.displayText)

        // Consume in Pro mode does not increment free counter
        scanManager.consumeScan(isPro = true)
        chatManager.incrementUsage(isPro = true)

        // Check free status again (Pro -> Free)
        val freeScanInfo = scanManager.getUsageInfo(isPro = false)
        val freeChatInfo = chatManager.getUsageInfo(isPro = false)

        assertEquals(2, freeScanInfo.scansUsed)
        assertEquals(8, freeScanInfo.scansRemaining)
        assertEquals(1, freeChatInfo.messagesUsedToday)
        assertEquals(14, freeChatInfo.messagesRemaining)
    }

    @Test
    fun `Judge Demo activation gives unlimited access via ViewModel isUserPro`() = runBlocking {
        val viewModel = CurbViewModel(app)
        ShadowLooper.idleMainLooper()

        // Initially free
        scanManager.consumeScan(isPro = false)
        viewModel.refreshUsageInfo()
        assertEquals(1, viewModel.scanUsageInfo.value.scansUsed)

        // Apply CURB26X
        viewModel.applyPromoCode("CURB26X")
        ShadowLooper.idleMainLooper()

        assertTrue(viewModel.isUserPro())
        assertTrue(viewModel.isJudgeProActive.value)
        assertTrue(viewModel.scanUsageInfo.value.isPro)
        assertEquals("Unlimited AI scans", viewModel.scanUsageInfo.value.displayText)
        assertTrue(viewModel.chatUsageInfo.value.isPro)
        assertEquals("Unlimited AI questions", viewModel.chatUsageInfo.value.displayText)
    }

    @Test
    fun `guest transition does NOT corrupt or reset scan and chat usage`() = runBlocking {
        val viewModel = CurbViewModel(app)
        ShadowLooper.idleMainLooper()

        scanManager.consumeScan(isPro = false)
        scanManager.consumeScan(isPro = false)
        scanManager.consumeScan(isPro = false)
        chatManager.incrementUsage(isPro = false)
        chatManager.incrementUsage(isPro = false)

        assertEquals(3, scanManager.getUsageInfo(isPro = false).scansUsed)
        assertEquals(2, chatManager.getUsageInfo(isPro = false).messagesUsedToday)

        var guestCompleted = false
        viewModel.startGuestSession { guestCompleted = true }
        ShadowLooper.idleMainLooper()

        assertTrue(guestCompleted)
        assertEquals(3, scanManager.getUsageInfo(isPro = false).scansUsed)
        assertEquals(7, scanManager.getUsageInfo(isPro = false).scansRemaining)
        assertEquals(2, chatManager.getUsageInfo(isPro = false).messagesUsedToday)
        assertEquals(13, chatManager.getUsageInfo(isPro = false).messagesRemaining)
    }

    @Test
    fun `scan count does not exceed monthly limit and remaining never becomes negative`() {
        repeat(15) {
            scanManager.consumeScan(isPro = false)
        }

        val info = scanManager.getUsageInfo(isPro = false)
        assertEquals(10, info.scansUsed)
        assertEquals(0, info.scansRemaining)
        assertTrue(info.isLimitReached)
        assertFalse(scanManager.canPerformScan(isPro = false))
    }

    @Test
    fun `chat count does not exceed daily limit and remaining never becomes negative`() {
        repeat(20) {
            chatManager.incrementUsage(isPro = false)
        }

        val info = chatManager.getUsageInfo(isPro = false)
        assertEquals(15, info.messagesUsedToday)
        assertEquals(0, info.messagesRemaining)
        assertTrue(info.isLimitReached)
        assertFalse(chatManager.canSendMessage(isPro = false))
    }
}
