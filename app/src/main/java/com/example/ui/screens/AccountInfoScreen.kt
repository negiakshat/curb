package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbOutline
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(
    userProfile: UserProfile,
    onSaveProfile: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var gender by remember { mutableStateOf(userProfile.gender) }
    var email by remember { mutableStateOf(userProfile.email) }
    var genderDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val genderOptions = listOf("Not specified", "Female", "Male", "Non-binary", "Prefer not to say")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("account_info_screen")
    ) {
        // TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("account_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CurbOnSurface
                )
            }
            Text(
                text = "Account Information",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CurbOnSurface
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // NAME
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_name_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CurbBlack,
                    unfocusedBorderColor = CurbOutline,
                    focusedContainerColor = CurbSurface,
                    unfocusedContainerColor = CurbSurface
                )
            )

            // GENDER DROPDOWN
            ExposedDropdownMenuBox(
                expanded = genderDropdownExpanded,
                onExpandedChange = { genderDropdownExpanded = !genderDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .testTag("account_gender_dropdown"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CurbBlack,
                        unfocusedBorderColor = CurbOutline,
                        focusedContainerColor = CurbSurface,
                        unfocusedContainerColor = CurbSurface
                    )
                )

                ExposedDropdownMenu(
                    expanded = genderDropdownExpanded,
                    onDismissRequest = { genderDropdownExpanded = false }
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                gender = option
                                genderDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // EMAIL
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("account_email_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CurbBlack,
                    unfocusedBorderColor = CurbOutline,
                    focusedContainerColor = CurbSurface,
                    unfocusedContainerColor = CurbSurface
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // DELETE ACCOUNT BUTTON
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("delete_account_button")
            ) {
                Text(
                    text = "Delete Account",
                    color = CurbError,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // SAVE CHANGES
            CurbPrimaryButton(
                text = "Save Changes",
                onClick = {
                    onSaveProfile(name, gender, email)
                    onBack()
                },
                testTag = "save_profile_button"
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold) },
            text = { Text("This will delete your local parking history, saved places, and account profile permanently.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onSaveProfile("Alex", "Not specified", "alex@example.com")
                        onBack()
                    }
                ) {
                    Text("Delete", color = CurbError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = CurbOnSurface)
                }
            }
        )
    }
}
