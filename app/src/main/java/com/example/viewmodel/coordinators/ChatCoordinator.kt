package com.example.viewmodel.coordinators

import com.example.data.local.ChatUsageManager
import com.example.data.local.SessionPreferences
import com.example.data.model.ChatMessage
import com.example.data.model.ScanResult
import com.example.data.remote.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatCoordinator(
    private val chatUsageManager: ChatUsageManager,
    private val sessionPreferences: SessionPreferences,
    private val coroutineScope: CoroutineScope
) {
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello ${sessionPreferences.getUserProfile().name}. I'm Curb AI, your dedicated parking assistant. Ask me anything about parking signs, curb colors, street cleaning schedules, or meter rules.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun canSendChatMessage(isUserPro: Boolean): Boolean {
        return chatUsageManager.canSendMessage(isUserPro)
    }

    fun sendChatMessage(
        query: String,
        isUserPro: Boolean,
        currentScan: ScanResult?,
        onLimitReached: () -> Unit = {}
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        if (_isChatLoading.value) return

        if (!chatUsageManager.canSendMessage(isUserPro)) {
            onLimitReached()
            return
        }

        val userMsg = ChatMessage(text = trimmed, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        chatUsageManager.incrementUsage(isUserPro)

        coroutineScope.launch {
            try {
                val responseText = GeminiService.askParkingAssistant(trimmed, _chatMessages.value, currentScan)
                val aiMsg = ChatMessage(text = responseText, isUser = false)
                _chatMessages.value = _chatMessages.value + aiMsg
            } catch (e: Exception) {
                val errMsg = ChatMessage(
                    text = "I'm having trouble connecting right now. Please try again in a moment.",
                    isUser = false
                )
                _chatMessages.value = _chatMessages.value + errMsg
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun resetChatMessages(userName: String = sessionPreferences.getUserProfile().name) {
        val greetingName = if (userName.isBlank() || userName == "Alex") "" else " $userName"
        _chatMessages.value = listOf(
            ChatMessage(
                text = "Hello$greetingName. I'm Curb AI, your dedicated parking assistant. Ask me anything about parking signs, curb colors, street cleaning schedules, or meter rules.",
                isUser = false
            )
        )
        _isChatLoading.value = false
    }
}
