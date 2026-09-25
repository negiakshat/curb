package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurbNote
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusNested

@Composable
fun CurbNoteDialog(
    initialText: String = "",
    isEditing: Boolean = false,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note", fontWeight = FontWeight.Bold, color = CurbOnSurface) },
            text = { Text("Are you sure you want to delete this note?", color = CurbOnSurfaceVariant) },
            containerColor = CurbSurface,
            shape = RoundedCornerShape(RadiusCard),
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("confirm_delete_note_button")
                ) {
                    Text("Delete", color = CurbError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = CurbOnSurfaceVariant)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(RadiusCard),
        containerColor = CurbSurface,
        title = {
            Text(
                text = if (isEditing) "Edit Note" else "Add Note",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CurbOnSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Add a note...", color = CurbOnSurfaceVariant) },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(RadiusNested),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CurbBlack,
                        unfocusedBorderColor = BentoBorder,
                        focusedTextColor = CurbOnSurface,
                        unfocusedTextColor = CurbOnSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_input_field")
                )

                if (isEditing && onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .align(Alignment.Start)
                            .testTag("delete_note_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Note",
                            tint = CurbError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Delete Note",
                            color = CurbError,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = text.trim()
                    if (trimmed.isNotEmpty()) {
                        onSave(trimmed)
                        onDismiss()
                    }
                },
                enabled = text.trim().isNotEmpty(),
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text(
                    text = "Save",
                    color = if (text.trim().isNotEmpty()) CurbBlack else CurbOnSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_note_button")
            ) {
                Text("Cancel", color = CurbOnSurfaceVariant)
            }
        }
    )
}

@Composable
fun CurbNoteSection(
    note: CurbNote?,
    isPro: Boolean,
    onAddOrEditNote: () -> Unit,
    onProLocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (note != null && note.text.isNotBlank()) {
        // DISPLAY EXISTING NOTE
        Surface(
            shape = RoundedCornerShape(RadiusNested),
            color = CurbSurfaceVariant,
            modifier = modifier
                .fillMaxWidth()
                .clickable {
                    if (isPro) {
                        onAddOrEditNote()
                    } else {
                        onProLocked()
                    }
                }
                .testTag("existing_note_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📝",
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Note",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CurbOnSurface
                        )
                    }

                    Text(
                        text = "Edit Note",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CurbBlack,
                        modifier = Modifier.clickable {
                            if (isPro) {
                                onAddOrEditNote()
                            } else {
                                onProLocked()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "\"${note.text}\"",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = CurbOnSurfaceVariant
                )
            }
        }
    } else {
        // NO NOTE YET -> ADD NOTE BUTTON
        Surface(
            shape = RoundedCornerShape(RadiusChip),
            color = if (isPro) CurbSurfaceVariant else BentoSand.copy(alpha = 0.6f),
            modifier = modifier
                .clickable {
                    if (isPro) {
                        onAddOrEditNote()
                    } else {
                        onProLocked()
                    }
                }
                .testTag("add_note_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isPro) Icons.Default.Add else Icons.Default.Lock,
                    contentDescription = "Add Note",
                    tint = CurbBlack,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "+ Add Note",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CurbBlack
                )
            }
        }
    }
}
