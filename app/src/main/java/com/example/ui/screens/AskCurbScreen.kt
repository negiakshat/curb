package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatUsageInfo
import com.example.data.model.ChatMessage
import com.example.ui.components.CurbLogo
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
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

@Composable
fun AskCurbScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    usageInfo: ChatUsageInfo = ChatUsageInfo(messagesUsedToday = 0),
    isPro: Boolean = false,
    onSendMessage: (String) -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val suggestedPrompts = listOf(
        "Can I park here after 6 PM?",
        "What does this sign mean?",
        "Does this apply on Sunday?",
        "What do the curb colors mean?"
    )

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
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(modifier = Modifier.width(4.dp))
                CurbLogo(symbolSize = 24.dp, fontSize = 18)
            }

            Surface(
                shape = RoundedCornerShape(RadiusSmall),
                color = if (!isPro && usageInfo.isLimitReached) CurbSurfacePeach else CurbSurfaceVariant
            ) {
                Text(
                    text = if (isPro) "AI Assistant" else usageInfo.displayText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbBlack,
                    modifier = Modifier
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("chat_usage_indicator")
                )
            }
        }

        // CHAT MESSAGE LIST
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
                            color = CurbBlack,
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

        if (!isPro && usageInfo.isLimitReached) {
            // POLISHED LIMIT REACHED STATE
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .testTag("chat_limit_reached_card"),
                shape = RoundedCornerShape(RadiusCard),
                color = CurbSurfacePeach
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
            // SUGGESTED PROMPTS
            if (messages.size <= 2) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestedPrompts) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(RadiusChip),
                            color = CurbSurfaceVariant,
                            modifier = Modifier
                                .clickable(enabled = !isLoading) {
                                    onSendMessage(prompt)
                                }
                                .testTag("suggested_prompt_$prompt")
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = CurbBlack,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // INPUT FIELD & SEND BUTTON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "Ask anything about parking…",
                            color = CurbOnSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(RadiusHero),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CurbBlack,
                        unfocusedBorderColor = CurbOutline,
                        focusedContainerColor = CurbSurface,
                        unfocusedContainerColor = CurbSurface,
                        focusedTextColor = CurbOnSurface,
                        unfocusedTextColor = CurbOnSurface
                    ),
                    maxLines = 3
                )

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
                        .size(48.dp)
                        .background(if (inputText.isNotBlank() && !isLoading) CurbBlack else CurbSurfaceVariant, CircleShape)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isLoading) CurbWhite else CurbOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
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
                    .size(28.dp)
                    .background(CurbBlack, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    color = CurbWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = RadiusNested,
                topEnd = RadiusNested,
                bottomStart = if (isUser) RadiusNested else 4.dp,
                bottomEnd = if (isUser) 4.dp else RadiusNested
            ),
            color = if (isUser) CurbBlack else CurbSurfaceVariant,
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
