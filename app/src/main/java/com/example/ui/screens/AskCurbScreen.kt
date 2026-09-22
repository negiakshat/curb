package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
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
import com.example.ui.theme.RadiusHero

private val curbGreetingHeadings = listOf(
    "where are we parking?",
    "found a spot yet?",
    "let’s check your parking.",
    "got a sign to decode?",
    "need the rule here?",
    "curious what that sign means?",
    "how long can you stay?",
    "when do you need to move?",
    "checking your time limit?",
    "is this spot actually safe?",
    "park here or keep moving?",
    "need a yes or no?",
    "where should you go next?",
    "ready to find your car?",
    "let’s sort this out.",
    "what’s happening out there?",
    "need a hand with anything?",
    "got something to figure out?"
)

private object CurbGreetingRotation {
    private var index = 0

    fun next(): String {
        val heading = curbGreetingHeadings[index]
        index = (index + 1) % curbGreetingHeadings.size
        return heading
    }
}

@Composable
private fun QuotaProgressRing(
    usageInfo: ChatUsageInfo,
    isPro: Boolean,
    modifier: Modifier = Modifier
) {
    if (isPro) {
        Surface(
            shape = RoundedCornerShape(RadiusChip),
            color = BentoSand,
            border = BorderStroke(1.dp, BentoBorder),
            modifier = modifier.testTag("chat_usage_indicator")
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
            modifier = modifier
                .size(34.dp)
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExhausted) CurbError else BentoTextPrimary
            )
        }
    }
}

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

    val greetingHeading = remember {
        CurbGreetingRotation.next()
    }

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

    // ROOT CONTAINER IS A BOX THAT OWNS THE IME INSET
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BentoCanvas)
            .statusBarsPadding()
            .imePadding()
            .testTag("ask_curb_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP APP BAR (COMPACT & CLEAN BENTO STYLE)
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
                            tint = BentoTextPrimary,
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
                            tint = BentoTextPrimary,
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
                                    tint = BentoWhite,
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = "AI",
                                    color = BentoWhite,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // QUOTA PROGRESS RING / PRO BADGE
                QuotaProgressRing(
                    usageInfo = usageInfo,
                    isPro = isPro
                )
            }

            // SCAN CONTEXT BANNER (COMPACT COLLAPSIBLE)
            if (scanResult != null) {
                val (verdictColor, verdictBgColor) = when (scanResult.verdict) {
                    ScanVerdict.ALLOWED -> Pair(CurbSuccess, CurbSuccessContainer)
                    ScanVerdict.RESTRICTED -> Pair(CurbError, CurbErrorContainer)
                    ScanVerdict.AMBIGUOUS -> Pair(CurbWarning, CurbWarningContainer)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(RadiusCard),
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

                        if (showScanDetailsExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = scanResult.explanation,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = BentoTextSecondary
                            )
                        }
                    }
                }
            }

            // MAIN CONTENT AREA: EMPTY STATE OR CHAT MESSAGES
            if (!hasUserMessages) {
                // QUESTION-FIRST EMPTY STATE: VERTICALLY CENTERED IN USABLE AREA
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // AI IDENTITY MARK
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BentoSand)
                                .border(1.dp, BentoBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Curb AI",
                                tint = BentoPrimaryDark,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // TWO-LINE PERSONALIZED GREETING (PROMINENT PROPORTIONS)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "Hi, $userName",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = greetingHeading,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoPrimaryDark,
                                lineHeight = 38.sp,
                                maxLines = 2
                            )
                        }

                        // VERTICAL SUGGESTED QUESTIONS
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Suggested questions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextSecondary,
                                letterSpacing = 0.5.sp
                            )

                            suggestedQuestions.forEach { question ->
                                Surface(
                                    shape = RoundedCornerShape(RadiusCard),
                                    color = BentoWhite,
                                    border = BorderStroke(1.dp, BentoBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(RadiusCard))
                                        .clickable(enabled = !isLoading) { onSendMessage(question) }
                                        .testTag("suggested_prompt_$question")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = question,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BentoTextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = BentoTextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
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
                                    color = BentoTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM INPUT / LIMIT CONTAINER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (!isPro && usageInfo.isLimitReached) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_limit_reached_card"),
                        shape = RoundedCornerShape(RadiusCard),
                        color = BentoPeach,
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
                                color = BentoPrimaryDark
                            )
                            Text(
                                text = "You've used your daily Curb AI questions. Limit resets tomorrow.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = BentoTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            CurbPrimaryButton(
                                text = "Get Curb Pro",
                                onClick = onUpgradeToPro,
                                testTag = "chat_upgrade_pro_button"
                            )
                        }
                    }
                } else {
                    // FLOATING COMPACT BENTO COMPOSER
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusHero),
                        color = BentoWhite,
                        border = BorderStroke(1.dp, BentoBorder),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Camera / Scan Action
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

                            // Input Text Field
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        text = "Ask Curb AI…",
                                        color = BentoTextSecondary,
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
                                    focusedTextColor = BentoTextPrimary,
                                    unfocusedTextColor = BentoTextPrimary
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
                                    tint = if (inputText.isNotBlank() && !isLoading) BentoWhite else BentoTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
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
                    tint = BentoWhite,
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
            color = if (isUser) BentoPrimaryDark else BentoWhite,
            border = if (!isUser) BorderStroke(1.dp, BentoBorder) else null,
            shadowElevation = if (!isUser) 0.5.dp else 0.dp,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.82f else 0.88f)
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = if (isUser) BentoWhite else BentoTextPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
