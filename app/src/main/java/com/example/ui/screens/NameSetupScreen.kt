package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CurbLogo
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbOutline
import com.example.ui.theme.CurbSurface

@Composable
fun NameSetupScreen(
    currentName: String,
    onNameSubmitted: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName.ifEmpty { "Alex" }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(16.dp))

            CurbLogo(symbolSize = 32.dp, fontSize = 24)

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "What should we call you?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your name helps us personalize your Curb experience.",
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = CurbOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(36.dp))

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Your name") },
                placeholder = { Text("Alex") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("name_input"),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CurbBlack,
                    unfocusedBorderColor = CurbOutline,
                    focusedContainerColor = CurbSurface,
                    unfocusedContainerColor = CurbSurface,
                    focusedLabelColor = CurbBlack,
                    unfocusedLabelColor = CurbOnSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onNameSubmitted(nameInput)
                    }
                )
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            CurbPrimaryButton(
                text = "Continue",
                onClick = {
                    onNameSubmitted(nameInput)
                },
                enabled = nameInput.isNotBlank(),
                testTag = "continue_button"
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
