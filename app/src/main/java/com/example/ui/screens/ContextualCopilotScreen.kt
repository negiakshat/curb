package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatUsageInfo
import com.example.data.model.ChatMessage
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbLogo
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.CurbWarningContainer
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContextualCopilotScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    scanResult: ScanResult?,
    usageInfo: ChatUsageInfo = ChatUsageInfo(messagesUsedToday = 0),
    isPro: Boolean = false,
    onSendMessage: (String) -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit
) {
    // 1. SCAN CONTEXT GUARD: If opened without a valid scanResult, pop back immediately
    if (scanResult == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val suggestedQuestions = remember(scanResult) {
        when (scanResult.verdict) {
            ScanVerdict.RESTRICTED -> listOf(
                "Can I park here right now?",
                "Why is this restricted?",
                "When CAN I park here?",
                "Is there a permit exemption?",
                "Where should I park instead?"
            )
            ScanVerdict.AMBIGUOUS -> listOf(
                "Can I park here right now?",
                "What makes the rule unclear?",
                "How do I verify this on-site?",
                "What does this sign mean?",
                "Is payment required?"
            )
            ScanVerdict.ALLOWED -> listOf(
                "Can I park here right now?",
                "How long can I stay?",
                "What happens after 6 PM?",
                "Is this allowed on Sunday?",
                "Is payment required?"
            )
        }
    }

    val hasUserMessages = remember(messages) {
        messages.any { it.isUser }
    }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // CRITICAL KEYBOARD INSET MANDATE:
    // This root Box is the ONLY container with .imePadding() in the entire screen.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .imePadding()
            .testTag("contextual_copilot_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("copilot_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BentoTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Curb Copilot",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                        }
                        Text(
                            text = "Based on this sign",
                            fontSize = 11.sp,
                            color = BentoTextSecondary
                        )
                    }
                }

                // Quota Ring or Pro Badge
                if (isPro) {
                    Surface(
                        shape = RoundedCornerShape(RadiusChip),
                        color = BentoSand,
                        border = BorderStroke(1.dp, BentoBorder)
                    ) {
                        Text(
                            text = "Pro",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimaryDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    val remaining = usageInfo.messagesRemaining
                    val limit = usageInfo.dailyLimit
                    val progress = if (limit > 0) (remaining.toFloat() / limit.toFloat()).coerceIn(0f, 1f) else 0f
                    val isExhausted = usageInfo.isLimitReached

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("chat_usage_indicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = if (isExhausted) CurbError else BentoPrimaryDark,
                            trackColor = BentoSand,
                            strokeWidth = 2.5.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = "$remaining",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExhausted) CurbError else BentoTextPrimary
                        )
                    }
                }
            }

            // 2. CHAT MESSAGES & CONTEXT AREA (weight 1f resizes cleanly when keyboard opens)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ITEM A: CURRENT SIGN CONTEXT CARD
                item {
                    val (verdictColor, verdictBgColor) = when (scanResult.verdict) {
                        ScanVerdict.ALLOWED -> Pair(CurbSuccess, CurbSuccessContainer)
                        ScanVerdict.RESTRICTED -> Pair(CurbError, CurbErrorContainer)
                        ScanVerdict.AMBIGUOUS -> Pair(CurbWarning, CurbWarningContainer)
                    }

                    val ruleSummary = scanResult.parkingRules.firstOrNull()?.ifBlank { null }
                        ?: scanResult.allowedUntilTime.ifBlank { null }
                        ?: scanResult.statusChipText

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("copilot_context_card"),
                        shape = RoundedCornerShape(RadiusCard),
                        color = BentoSand,
                        border = BorderStroke(1.dp, BentoBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = BentoPrimaryDark
                                ) {
                                    Text(
                                        text = "CURRENT SIGN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoWhite,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(RadiusChip),
                                    color = verdictBgColor
                                ) {
                                    Text(
                                        text = scanResult.verdict.displayTitle,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = verdictColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = BentoPrimaryDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = scanResult.locationName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = ruleSummary,
                                fontSize = 12.sp,
                                color = BentoTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // ITEM B: SUGGESTED QUESTIONS CHIPS (When starting or above messages)
                if (!hasUserMessages) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Suggested questions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextSecondary,
                                letterSpacing = 0.5.sp
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                suggestedQuestions.forEach { question ->
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = BentoWhite,
                                        border = BorderStroke(1.dp, BentoBorder),
                                        modifier = Modifier
                                            .clickable { onSendMessage(question) }
                                            .testTag("suggestion_chip")
                                    ) {
                                        Text(
                                            text = question,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BentoPrimaryDark,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ITEM C: MESSAGES LIST
                items(messages) { msg ->
                    if (msg.isUser) {
                        // User message bubble (Right side)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 18.dp,
                                    bottomEnd = 4.dp
                                ),
                                color = BentoPeach,
                                border = BorderStroke(1.dp, BentoBorder),
                                modifier = Modifier
                                    .padding(start = 48.dp)
                                    .testTag("user_message_bubble")
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = BentoTextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    } else {
                        // Copilot message bubble (Left side)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(BentoSand)
                                    .border(1.dp, BentoBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CurbLogo(
                                    symbolSize = 14.dp,
                                    fontSize = 0,
                                    tint = BentoPrimaryDark
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 18.dp,
                                    bottomEnd = 18.dp
                                ),
                                color = BentoWhite,
                                border = BorderStroke(1.dp, BentoBorder),
                                modifier = Modifier
                                    .padding(end = 36.dp)
                                    .testTag("copilot_message_bubble")
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = BentoTextPrimary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(BentoSand)
                                    .border(1.dp, BentoBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                CurbLogo(
                                    symbolSize = 14.dp,
                                    fontSize = 0,
                                    tint = BentoPrimaryDark
                                )
                            }
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = BentoPrimaryDark,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Analyzing sign context...",
                                fontSize = 12.sp,
                                color = BentoTextSecondary
                            )
                        }
                    }
                }
            }

            // 3. COMPOSER BAR (NO extra imePadding, NO navigationBarsPadding, NO bottom spacers)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = BentoWhite,
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Ask a follow-up question...",
                                color = BentoTextSecondary,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    val query = inputText.trim()
                                    inputText = ""
                                    onSendMessage(query)
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copilot_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = BentoTextPrimary,
                            unfocusedTextColor = BentoTextPrimary
                        )
                    )

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val query = inputText.trim()
                                inputText = ""
                                onSendMessage(query)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (inputText.isNotBlank() && !isLoading) BentoPrimaryDark else BentoSand,
                                CircleShape
                            )
                            .testTag("copilot_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isLoading) BentoWhite else BentoTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
