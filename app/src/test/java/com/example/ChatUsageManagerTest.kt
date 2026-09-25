package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChatUsageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatUsageManagerTest {

    private lateinit var context: Context
    private lateinit var usageManager: ChatUsageManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        usageManager = ChatUsageManager(context)
        usageManager.resetUsage()
    }

    @Test
    fun `fresh free user starts with 15 daily questions`() {
        val info = usageManager.getUsageInfo(isPro = false)
        assertEquals(0, info.messagesUsedToday)
        assertEquals(15, info.messagesRemaining)
        assertFalse(info.isLimitReached)
        assertTrue(usageManager.canSendMessage(isPro = false))
        assertEquals("15 AI questions left today", info.displayText)
    }

    @Test
    fun `submitting message increments usage count`() {
        val updated = usageManager.incrementUsage(isPro = false)
        assertEquals(1, updated.messagesUsedToday)
        assertEquals(14, updated.messagesRemaining)
        assertFalse(updated.isLimitReached)
        assertTrue(usageManager.canSendMessage(isPro = false))
        assertEquals("14 AI questions left today", updated.displayText)
    }

    @Test
    fun `one message remaining displays singular form`() {
        repeat(14) {
            usageManager.incrementUsage(isPro = false)
        }
        val info = usageManager.getUsageInfo(isPro = false)
        assertEquals(14, info.messagesUsedToday)
        assertEquals(1, info.messagesRemaining)
        assertFalse(info.isLimitReached)
        assertTrue(usageManager.canSendMessage(isPro = false))
        assertEquals("1 AI question left today", info.displayText)
    }

    @Test
    fun `hitting 15 messages triggers limit reached and blocks further messages`() {
        repeat(15) {
            usageManager.incrementUsage(isPro = false)
        }
        val info = usageManager.getUsageInfo(isPro = false)
        assertEquals(15, info.messagesUsedToday)
        assertEquals(0, info.messagesRemaining)
        assertTrue(info.isLimitReached)
        assertFalse(usageManager.canSendMessage(isPro = false))
        assertEquals("0 AI questions left today", info.displayText)
    }

    @Test
    fun `pro user has unlimited messages and is never blocked`() {
        repeat(15) {
            usageManager.incrementUsage(isPro = false)
        }
        // Check with isPro = true
        val infoPro = usageManager.getUsageInfo(isPro = true)
        assertFalse(infoPro.isLimitReached)
        assertTrue(usageManager.canSendMessage(isPro = true))
        assertEquals("Unlimited AI questions", infoPro.displayText)
    }

    @Test
    fun `usage count persists across new ChatUsageManager instances`() {
        usageManager.incrementUsage(isPro = false)
        usageManager.incrementUsage(isPro = false)

        val newInstance = ChatUsageManager(context)
        val info = newInstance.getUsageInfo(isPro = false)
        assertEquals(2, info.messagesUsedToday)
        assertEquals(13, info.messagesRemaining)
    }

    @Test
    fun `resetUsage clears usage counter to zero`() {
        repeat(5) {
            usageManager.incrementUsage(isPro = false)
        }
        usageManager.resetUsage()

        val info = usageManager.getUsageInfo(isPro = false)
        assertEquals(0, info.messagesUsedToday)
        assertEquals(15, info.messagesRemaining)
        assertTrue(usageManager.canSendMessage(isPro = false))
    }
}
