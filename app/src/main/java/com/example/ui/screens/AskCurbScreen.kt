package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.data.local.ChatUsageInfo
import com.example.data.model.ChatMessage
import com.example.data.model.ScanResult

@Composable
fun AskCurbScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    scanResult: ScanResult? = null,
    usageInfo: ChatUsageInfo = ChatUsageInfo(messagesUsedToday = 0),
    isPro: Boolean = false,
    userName: String = "Alex",
    onSendMessage: (String) -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit
) {
    ContextualCopilotScreen(
        messages = messages,
        isLoading = isLoading,
        scanResult = scanResult,
        usageInfo = usageInfo,
        isPro = isPro,
        onSendMessage = onSendMessage,
        onUpgradeToPro = onUpgradeToPro,
        onBack = onBack
    )
}
