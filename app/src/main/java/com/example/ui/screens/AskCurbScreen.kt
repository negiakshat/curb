package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatUsageInfo
import com.example.data.model.ChatMessage
import com.example.data.model.ScanResult
import com.example.data.model.ScanVerdict
import com.example.ui.components.CurbLogo
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextDark
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfacePeach
import com.example.ui.theme.CurbWhite
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusSmall

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
    var inputText by remember { mutableStateOf("") }
    var showScanDetailsExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val suggestedQuestions = remember(scanResult) {
        if (scanResult != null) {
            when (scanResult.verdict) {
                ScanVerdict.RESTRICTED -> listOf(
                    "Why can't I park here?",
                    "When CAN I park here?",
                    "Which sign restricts parking?",
                    "Are there permit exemptions?"
                )
                ScanVerdict.AMBIGUOUS -> listOf(
                    "What makes the rule unclear?",
                    "How to verify on-site?",
                    "What sign detail is missing?",
                    "Explain visible signs"
                )
                ScanVerdict.ALLOWED -> listOf(
                    "When do I need to move?",
                    "Are there upcoming limits?",
                    "Is payment required?",
                    "Summarize spot rules"
                )
            }
        } else {
            listOf(
                "Can I park here?",
                "How long can I stay?",
                "What does this sign mean?",
                "When do I need to move?"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .imePadding()
            .testTag("ask_curb_screen")
    ) {
        // TOP APP BAR (COMPACT & CLEAN)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("ask_curb_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CurbOnSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CurbLogo(
                        symbolSize = 18.dp,
                        fontSize = 16,
                        tint = CurbOnSurface,
                        wordmark = "CURB"
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BentoPrimaryDark
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = CurbWhite,
                                modifier = Modifier.size(9.dp)
                            )
                            Text(
                                text = "AI",
                                color = CurbWhite,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(RadiusSmall),
                color = if (!isPro && usageInfo.isLimitReached) CurbSurfacePeach else BentoSand,
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Text(
                    text = if (isPro) "Pro" else usageInfo.displayText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = BentoTextDark,
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("chat_usage_indicator")
                )
            }
        }

        // SCAN CONTEXT BANNER (COMPACT COLLAPSIBLE)
        if (scanResult != null) {
            val (verdictColor, verdictBgColor) = when (scanResult.verdict) {
                ScanVerdict.ALLOWED -> Pair(Color(0xFF1B5E20), Color(0xFFE8F5E9))
                ScanVerdict.RESTRICTED -> Pair(Color(0xFFB71C1C), Color(0xFFFFEBEE))
                ScanVerdict.AMBIGUOUS -> Pair(Color(0xFFE65100), Color(0xFFFFF3E0))
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = BentoSand,
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showScanDetailsExpanded = !showScanDetailsExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = BentoPrimaryDark,
                                modifier = Modifier.size(15.dp)
                            )

                            Text(
                                text = scanResult.locationName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BentoPrimaryDark,
                                maxLines = 1
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
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

                    if (showScanDetailsExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = scanResult.explanation,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = CurbOnSurfaceVariant
                        )
                    }
                }
            }
        }

        // MAIN CONTENT AREA: MINIMAL CHAT-FIRST / QUESTION-FIRST LANDING OR CHAT MESSAGES
        if (!hasUserMessages) {
            // QUESTION-FIRST EMPTY STATE: TOP-ANCHORED, COMPACT VISUAL HIERARCHY (NO LARGE BLANK VOID)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // AI IDENTITY MARK (reuses existing CurbLogo symbol)
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(BentoSand)
                        .border(1.dp, BentoBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Curb AI",
                        tint = BentoPrimaryDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // HEADLINE + SUBLINE (M3 TYPE HIERARCHY)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ask Curb AI is ready",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (scanResult != null) {
                            "Ask about parking, signs, or rules at ${scanResult.locationName} (${scanResult.verdict.displayTitle})."
                        } else {
                            "Ask about parking, signs, or rules — or pick a suggestion below."
                        },
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = CurbOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // SUGGESTED QUESTIONS (FILLED-TONE CHIPS, WRAPPED IN FLOW)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Suggested questions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CurbOnSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        suggestedQuestions.forEach { question ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = CurbSurface,
                                border = BorderStroke(1.dp, BentoBorder),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(enabled = !isLoading) { onSendMessage(question) }
                                    .testTag("suggested_prompt_$question")
                            ) {
                                Text(
                                    text = question,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BentoPrimaryDark,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // CHAT MESSAGES LIST
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = BentoPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Curb AI is thinking…",
                                fontSize = 12.sp,
                                color = CurbOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // LIMIT REACHED WARNING OR PROMINENT INPUT BAR
        if (!isPro && usageInfo.isLimitReached) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("chat_limit_reached_card"),
                shape = RoundedCornerShape(RadiusCard),
                color = CurbSurfacePeach,
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Daily limit reached",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbBlack
                    )
                    Text(
                        text = "You've used your daily Curb AI questions. Limit resets tomorrow.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = CurbOnSurfaceVariant
                    )
                    CurbPrimaryButton(
                        text = "Get Curb Pro",
                        onClick = onUpgradeToPro,
                        testTag = "chat_upgrade_pro_button"
                    )
                }
            }
        } else {
            // PROMINENT CLEAN INPUT BAR
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = CurbSurface,
                border = BorderStroke(1.5.dp, BentoBorder),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Attachment / Camera Action Icon
                    IconButton(
                        onClick = {
                            onSendMessage("I want to scan a parking sign or curb")
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan sign",
                            tint = BentoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Input Text Box
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "Ask Curb AI…",
                                color = CurbOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = CurbOnSurface,
                            unfocusedTextColor = CurbOnSurface
                        ),
                        maxLines = 4
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val text = inputText
                                inputText = ""
                                onSendMessage(text)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (inputText.isNotBlank() && !isLoading) BentoPrimaryDark else BentoSand,
                                CircleShape
                            )
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isLoading) CurbWhite else CurbOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(BentoPrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Curb AI",
                    tint = CurbWhite,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) BentoPrimaryDark else CurbSurface,
            border = if (!isUser) BorderStroke(1.dp, BentoBorder) else null,
            shadowElevation = if (!isUser) 0.5.dp else 0.dp,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.88f)
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = if (isUser) CurbWhite else CurbOnSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

