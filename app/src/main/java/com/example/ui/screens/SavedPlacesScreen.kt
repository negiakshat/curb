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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
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
import com.example.data.model.CurbNote
import com.example.data.model.SavedPlace
import com.example.ui.components.CurbCard
import com.example.ui.components.CurbNoteDialog
import com.example.ui.components.CurbNoteSection
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbProFeatureBottomSheet
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbOutline
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWhite

@Composable
fun SavedPlacesScreen(
    savedPlaces: List<SavedPlace>,
    notes: List<CurbNote> = emptyList(),
    isPro: Boolean = false,
    onAddPlace: (String, String, String) -> Unit,
    onDeletePlace: (Long) -> Unit,
    onSaveNote: (targetType: String, targetId: Long, text: String) -> Unit = { _, _, _ -> },
    onDeleteNote: (targetType: String, targetId: Long) -> Unit = { _, _ -> },
    onScanPlace: (SavedPlace) -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showProSheet by remember { mutableStateOf(false) }
    var showNotesProSheet by remember { mutableStateOf(false) }
    var editingPlaceIdForNote by remember { mutableStateOf<Long?>(null) }
    var newPlaceName by remember { mutableStateOf("") }
    var newPlaceAddress by remember { mutableStateOf("") }
    var newPlaceNote by remember { mutableStateOf("") }

    val handleAddRequest = {
        if (!isPro && savedPlaces.size >= 3) {
            showProSheet = true
        } else {
            showAddDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("saved_places_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
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
                        modifier = Modifier.testTag("saved_places_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CurbOnSurface
                        )
                    }
                    Text(
                        text = "Saved Places",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CurbOnSurface
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (savedPlaces.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp)
                        ) {
                            CurbCard(
                                cornerRadius = RadiusCard,
                                backgroundColor = CurbSurface
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = CurbOnSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No saved places yet",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CurbOnSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Save your frequent parking spots like Home or Work for fast rule checks.",
                                        fontSize = 13.sp,
                                        color = CurbOnSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(savedPlaces) { place ->
                        val placeNote = notes.firstOrNull {
                            it.targetType == CurbNote.TARGET_SAVED_PLACE && it.targetId == place.id
                        }

                        CurbCard(
                            cornerRadius = RadiusCard,
                            backgroundColor = CurbSurface,
                            onClick = { onScanPlace(place) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(CurbSurfaceVariant, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = CurbBlack,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = place.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CurbOnSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = place.address,
                                                fontSize = 13.sp,
                                                color = CurbOnSurfaceVariant
                                            )
                                            if (place.parkingNote.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = place.parkingNote,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = CurbBlack
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeletePlace(place.id) },
                                        modifier = Modifier.testTag("delete_place_${place.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = CurbOnSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // NOTES SECTION FOR THIS SAVED PLACE
                                CurbNoteSection(
                                    note = placeNote,
                                    isPro = isPro,
                                    onAddOrEditNote = {
                                        editingPlaceIdForNote = place.id
                                    },
                                    onProLocked = {
                                        showNotesProSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ADD PLACE FAB
        FloatingActionButton(
            onClick = handleAddRequest,
            containerColor = CurbBlack,
            contentColor = CurbWhite,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_place_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Place"
            )
        }
    }

    if (showProSheet) {
        CurbProFeatureBottomSheet(
            title = "Unlock Unlimited Saved Places",
            supportingText = "Save as many parking locations as you need with Curb Pro.",
            icon = Icons.Default.Bookmark,
            onGetPro = onUpgradeToPro,
            onDismiss = { showProSheet = false }
        )
    }

    if (showNotesProSheet) {
        CurbProFeatureBottomSheet(
            title = "Save Notes with Curb Pro",
            supportingText = "Keep personal reminders with your saved places and parking scans.",
            icon = Icons.Default.Edit,
            onGetPro = onUpgradeToPro,
            onDismiss = { showNotesProSheet = false }
        )
    }

    if (editingPlaceIdForNote != null) {
        val targetPlaceId = editingPlaceIdForNote!!
        val currentNote = notes.firstOrNull {
            it.targetType == CurbNote.TARGET_SAVED_PLACE && it.targetId == targetPlaceId
        }

        CurbNoteDialog(
            initialText = currentNote?.text ?: "",
            isEditing = currentNote != null,
            onSave = { newText ->
                onSaveNote(CurbNote.TARGET_SAVED_PLACE, targetPlaceId, newText)
                editingPlaceIdForNote = null
            },
            onDelete = if (currentNote != null) {
                {
                    onDeleteNote(CurbNote.TARGET_SAVED_PLACE, targetPlaceId)
                    editingPlaceIdForNote = null
                }
            } else null,
            onDismiss = {
                editingPlaceIdForNote = null
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            shape = RoundedCornerShape(RadiusCard),
            containerColor = CurbSurface,
            title = { Text("Add Saved Place", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPlaceName,
                        onValueChange = { newPlaceName = it },
                        label = { Text("Place Name (e.g. Home, Office)") },
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusNested),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPlaceAddress,
                        onValueChange = { newPlaceAddress = it },
                        label = { Text("Address / Cross Street") },
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusNested),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPlaceNote,
                        onValueChange = { newPlaceNote = it },
                        label = { Text("Parking Note (e.g. Permit A)") },
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusNested),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaceName.isNotBlank()) {
                            if (!isPro && savedPlaces.size >= 3) {
                                showAddDialog = false
                                showProSheet = true
                            } else {
                                onAddPlace(newPlaceName, newPlaceAddress, newPlaceNote)
                                newPlaceName = ""
                                newPlaceAddress = ""
                                newPlaceNote = ""
                                showAddDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save Place", color = CurbBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = CurbOnSurfaceVariant)
                }
            }
        )
    }
}
