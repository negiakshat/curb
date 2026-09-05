package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextDark
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbOutline
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfacePeach
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite

data class PromptCardItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val promptText: String
)

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

    val quickPrompts = remember(scanResult) {
        if (scanResult != null) {
            when (scanResult.verdict) {
                ScanVerdict.RESTRICTED -> listOf(
                    PromptCardItem(
                        title = "Why can't I park here?",
                        description = "Explain active restriction",
                        icon = Icons.Default.DirectionsCar,
                        promptText = "Why can't I park at ${scanResult.locationName} right now?"
                    ),
                    PromptCardItem(
                        title = "Which sign restricts parking?",
                        description = "Identify prohibiting sign",
                        icon = Icons.Default.Layers,
                        promptText = "Which physical sign is restricting parking right now?"
                    ),
                    PromptCardItem(
                        title = "When CAN I park here?",
                        description = "Find next allowed window",
                        icon = Icons.Default.Place,
                        promptText = "When does this restriction end and when can I park here?"
                    ),
                    PromptCardItem(
                        title = "Are there exemptions?",
                        description = "Check permits or exceptions",
                        icon = Icons.Default.AutoAwesome,
                        promptText = "Are there any permit or vehicle exemptions for this restriction?"
                    )
                )
                ScanVerdict.AMBIGUOUS -> listOf(
                    PromptCardItem(
                        title = "What makes rule unclear?",
                        description = "Understand the uncertainty",
                        icon = Icons.Default.DirectionsCar,
                        promptText = "Why couldn't Curb establish the active rule with certainty?"
                    ),
                    PromptCardItem(
                        title = "What evidence is missing?",
                        description = "Photo & sign clarity advice",
                        icon = Icons.Default.Layers,
                        promptText = "What additional sign evidence or photo detail is needed?"
                    ),
                    PromptCardItem(
                        title = "How to verify on-site?",
                        description = "Physical signage check guide",
                        icon = Icons.Default.Place,
                        promptText = "How should I safely verify this spot on-site before parking?"
                    ),
                    PromptCardItem(
                        title = "Explain visible signs",
                        description = "Break down each plate read",
                        icon = Icons.Default.AutoAwesome,
                        promptText = "Explain what each detected sign plate means."
                    )
                )
                ScanVerdict.ALLOWED -> listOf(
                    PromptCardItem(
                        title = "When do I need to move?",
                        description = "Time limit & end window",
                        icon = Icons.Default.DirectionsCar,
                        promptText = "How long can I stay here and when do I need to move my car?"
                    ),
                    PromptCardItem(
                        title = "Are there upcoming limits?",
                        description = "Check street sweep or tow",
                        icon = Icons.Default.Layers,
                        promptText = "Are there any upcoming street sweeping or tow-away restrictions?"
                    ),
                    PromptCardItem(
                        title = "Is payment required?",
                        description = "Meter & station guidance",
                        icon = Icons.Default.Place,
                        promptText = "Is payment or meter activation required at this spot?"
                    ),
                    PromptCardItem(
                        title = "Summarize spot rules",
                        description = "Quick full recap",
                        icon = Icons.Default.AutoAwesome,
                        promptText = "Summarize all active rules for this parking spot."
                    )
                )
            }
        } else {
            listOf(
                PromptCardItem(
                    title = "Can I park here?",
                    description = "Check current meter or street rules",
                    icon = Icons.Default.DirectionsCar,
                    promptText = "Can I park here right now?"
                ),
                PromptCardItem(
                    title = "Explain this parking sign",
                    description = "Understand complex restrictions",
                    icon = Icons.Default.Layers,
                    promptText = "Explain this parking sign and its active rules"
                ),
                PromptCardItem(
                    title = "Find parking near me",
                    description = "Locate open curb spots nearby",
                    icon = Icons.Default.Place,
                    promptText = "Find parking options near my current location"
                ),
                PromptCardItem(
                    title = "Ask anything",
                    description = "Get instant AI parking guidance",
                    icon = Icons.Default.AutoAwesome,
                    promptText = "What parking rules or curb colors should I watch out for?"
                )
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
            .navigationBarsPadding()
            .imePadding()
            .testTag("ask_curb_screen")
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("ask_curb_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CurbOnSurface
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurbLogo(
                            symbolSize = 20.dp,
                            fontSize = 17,
                            tint = CurbOnSurface,
                            wordmark = "CURB"
                        )

                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = BentoPrimaryDark
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "AI Parking Assistant",
                        fontSize = 11.sp,
                        color = CurbOnSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(RadiusSmall),
                color = if (!isPro && usageInfo.isLimitReached) CurbSurfacePeach else BentoSand,
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Text(
                    text = if (isPro) "Pro Member" else usageInfo.displayText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BentoTextDark,
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("chat_usage_indicator")
                )
            }
        }

        // SCAN CONTEXT BANNER
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
                shape = RoundedCornerShape(12.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = BentoPrimaryDark,
                                modifier = Modifier.size(16.dp)
                            )

                            Column {
                                Text(
                                    text = scanResult.locationName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BentoPrimaryDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${scanResult.detectedSigns.size} physical sign(s) attached",
                                    fontSize = 10.sp,
                                    color = CurbOnSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = verdictBgColor
                        ) {
                            Text(
                                text = scanResult.verdict.displayTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (showScanDetailsExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(BentoBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Scan Summary for Curb AI:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scanResult.explanation,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = CurbOnSurfaceVariant
                        )

                        if (scanResult.detectedSigns.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            for (idx in scanResult.detectedSigns.indices) {
                                val sign = scanResult.detectedSigns[idx]
                                Text(
                                    text = "• Sign #${idx + 1}: ${sign.title} — ${sign.subtitle}",
                                    fontSize = 10.sp,
                                    color = CurbOnSurface,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // MAIN CONTENT AREA (EMPTY LANDING VS CHAT MESSAGES)
        if (!hasUserMessages) {
            // GEMINI-INSPIRED MINIMAL EMPTY STATE LANDING
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Sparkle Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BentoSand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Curb AI",
                        tint = BentoPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Greeting
                Text(
                    text = if (scanResult != null) "Asking about ${scanResult.locationName}" else "Hello, $userName",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BentoPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Main Heading
                Text(
                    text = if (scanResult != null) "What would you like to clarify?" else "Where should we start?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbOnSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Supporting Text
                Text(
                    text = if (scanResult != null) {
                        "Curb AI has loaded the scan context (${scanResult.detectedSigns.size} signs, verdict: ${scanResult.verdict.displayTitle}). Ask any follow-up question below."
                    } else {
                        "Ask me anything about parking, signs, meters, or your scans."
                    },
                    fontSize = 13.sp,
                    color = CurbOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // QUICK PROMPT CARDS GRID (2x2)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (i in 0 until quickPrompts.size step 2) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val card1 = quickPrompts[i]
                            val card2 = quickPrompts.getOrNull(i + 1)

                            QuickPromptCard(
                                item = card1,
                                isLoading = isLoading,
                                onSelect = { onSendMessage(card1.promptText) },
                                modifier = Modifier.weight(1f)
                            )

                            if (card2 != null) {
                                QuickPromptCard(
                                    item = card2,
                                    isLoading = isLoading,
                                    onSelect = { onSendMessage(card2.promptText) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // CHAT MESSAGE LIST (CONVERSATION MODE)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg)
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = BentoPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Curb AI is thinking…",
                                fontSize = 13.sp,
                                color = CurbOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // LIMIT REACHED WARNING OR INPUT BAR
        if (!isPro && usageInfo.isLimitReached) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .testTag("chat_limit_reached_card"),
                shape = RoundedCornerShape(RadiusCard),
                color = CurbSurfacePeach,
                border = BorderStroke(1.dp, BentoBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Curb AI daily limit reached",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbBlack
                    )
                    Text(
                        text = "You've used all 15 Curb AI questions for today. Your limit resets tomorrow.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = CurbOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Button(
                        onClick = onUpgradeToPro,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CurbBlack,
                            contentColor = CurbWhite
                        ),
                        shape = RoundedCornerShape(RadiusHero),
                        modifier = Modifier.testTag("chat_upgrade_pro_button")
                    ) {
                        Text(
                            text = "Get Curb Pro",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // GEMINI-INSPIRED ROUNDED INPUT BAR AT BOTTOM
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(28.dp),
                color = CurbSurface,
                border = BorderStroke(1.dp, BentoBorder),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                text = "Ask Curb AI...",
                                color = CurbOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(20.dp),
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
                            .size(40.dp)
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickPromptCard(
    item: PromptCardItem,
    isLoading: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CurbSurface,
        border = BorderStroke(1.dp, BentoBorder),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading, onClick = onSelect)
            .testTag("suggested_prompt_${item.title}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BentoSand),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = BentoPrimaryDark,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface,
                lineHeight = 17.sp
            )

            Text(
                text = item.description,
                fontSize = 11.sp,
                color = CurbOnSurfaceVariant,
                lineHeight = 15.sp
            )
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BentoPrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Curb AI",
                    tint = CurbWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = if (isUser) BentoPrimaryDark else CurbSurface,
            border = if (!isUser) BorderStroke(1.dp, BentoBorder) else null,
            shadowElevation = if (!isUser) 1.dp else 0.dp,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.8f else 0.88f)
        ) {
            Text(
                text = message.text,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = if (isUser) CurbWhite else CurbOnSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}
