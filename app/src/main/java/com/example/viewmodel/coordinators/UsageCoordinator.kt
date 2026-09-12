package com.example.viewmodel.coordinators

import com.example.data.local.ChatUsageInfo
import com.example.data.local.ChatUsageManager
import com.example.data.local.ScanUsageInfo
import com.example.data.local.ScanUsageManager
import com.example.data.local.SessionPreferences
import com.example.data.model.UserProfile
import com.example.data.remote.SubscriptionService
import com.example.data.remote.SubscriptionUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UsageCoordinator(
    private val scanUsageManager: ScanUsageManager,
    private val chatUsageManager: ChatUsageManager,
    private val subscriptionService: SubscriptionService,
    private val sessionPreferences: SessionPreferences,
    private val isJudgeProActiveFlow: StateFlow<Boolean>,
    private val userProfileFlow: StateFlow<UserProfile>,
    private val coroutineScope: CoroutineScope
) {
    val subscriptionState: StateFlow<SubscriptionUiState> = subscriptionService.subscriptionState

    val isUserPro: StateFlow<Boolean> = combine(
        subscriptionState,
        isJudgeProActiveFlow,
        userProfileFlow
    ) { subState, judgeActive, profile ->
        if (subState.isConfigured) {
            subState.isPro || judgeActive
        } else {
            subState.isPro || judgeActive || profile.isPro
        }
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = checkIsUserPro()
    )

    fun checkIsUserPro(): Boolean {
        val subState = subscriptionService.subscriptionState.value
        val judgeActive = isJudgeProActiveFlow.value
        val profile = userProfileFlow.value
        return if (subState.isConfigured) {
            subState.isPro || judgeActive
        } else {
            subState.isPro || judgeActive || profile.isPro
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    val scanUsageInfo: StateFlow<ScanUsageInfo> = combine(isUserPro, _refreshTrigger) { pro, _ ->
        scanUsageManager.getUsageInfo(pro)
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = scanUsageManager.getUsageInfo(checkIsUserPro())
    )

    val chatUsageInfo: StateFlow<ChatUsageInfo> = combine(isUserPro, _refreshTrigger) { pro, _ ->
        chatUsageManager.getUsageInfo(pro)
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = chatUsageManager.getUsageInfo(checkIsUserPro())
    )

    fun canPerformScan(): Boolean {
        return scanUsageManager.canPerformScan(checkIsUserPro())
    }

    fun canSendChatMessage(): Boolean {
        return chatUsageManager.canSendMessage(checkIsUserPro())
    }

    fun refreshUsageInfo() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun updateUsageForReset(isPro: Boolean = false) {
        _refreshTrigger.value = System.currentTimeMillis()
    }
}
