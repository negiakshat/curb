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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurbNote
import com.example.data.model.SavedPlace
import com.example.ui.components.CurbNoteDialog
import com.example.ui.components.CurbNoteSection
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbProFeatureBottomSheet
import com.example.ui.theme.BentoBeige
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextDark
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested

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
            .background(BentoCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("saved_places_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP HEADER BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BentoSand)
                            .clickable { onBack() }
                            .testTag("saved_places_back_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BentoPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Saved Places",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoTextPrimary,
                            lineHeight = 26.sp
                        )
                        Text(
                            text = "Your parking cheat sheet — minus the screenshot graveyard.",
                            fontSize = 11.sp,
                            color = BentoTextSecondary,
                            lineHeight = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // STATUS CHIP
                Surface(
                    shape = RoundedCornerShape(RadiusChip),
                    color = if (isPro) BentoPeach else BentoSand
                ) {
                    Text(
                        text = if (isPro) "${savedPlaces.size} saved • Pro" else "${savedPlaces.size}/3 free spots",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimaryDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // CONTENT: EMPTY STATE OR 2-COLUMN BENTO GRID
            if (savedPlaces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(RadiusHero),
                        colors = CardDefaults.cardColors(containerColor = BentoWhite),
                        border = BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(BentoSand),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = BentoPrimaryDark,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No saved places yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Save your frequent parking spots like Home or Work for fast rule checks.",
                                fontSize = 13.sp,
                                color = BentoTextSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            CurbPrimaryButton(
                                text = "ADD FIRST PLACE",
                                onClick = handleAddRequest,
                                backgroundColor = BentoPrimaryDark,
                                contentColor = BentoWhite,
                                leadingIcon = Icons.Default.Add,
                                testTag = "add_first_place_button"
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedPlaces, key = { it.id }) { place ->
                        val placeNote = notes.firstOrNull {
                            it.targetType == CurbNote.TARGET_SAVED_PLACE && it.targetId == place.id
                        }

                        Card(
                            shape = RoundedCornerShape(RadiusCard),
                            colors = CardDefaults.cardColors(containerColor = BentoWhite),
                            border = BorderStroke(1.dp, BentoBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onScanPlace(place) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // TOP ROW: ICON + DELETE
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(BentoSand),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = BentoPrimaryDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeletePlace(place.id) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_place_${place.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = BentoTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // MIDDLE SECTION: NAME, ADDRESS, PARKING NOTE
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = place.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = place.address,
                                        fontSize = 12.sp,
                                        color = BentoTextSecondary,
                                        lineHeight = 15.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (place.parkingNote.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(RadiusNested),
                                            color = BentoBeige,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = place.parkingNote,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = BentoTextDark,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }

                                // NOTES SECTION
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

                                // PRIMARY ACTION: CHECK RULES
                                Surface(
                                    shape = RoundedCornerShape(RadiusChip),
                                    color = BentoSand,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onScanPlace(place) }
                                        .testTag("check_rules_${place.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = BentoPrimaryDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CHECK RULES",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            color = BentoPrimaryDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FLOATING ADD ACTION (WHEN PLACES EXIST)
        if (savedPlaces.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(RadiusHero),
                color = BentoPrimaryDark,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .clickable { handleAddRequest() }
                    .testTag("add_place_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Place",
                        tint = BentoWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ADD PLACE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = BentoWhite
                    )
                }
            }
        }
    }

    // PRO SHEETS AND DIALOGS
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
            containerColor = BentoWhite,
            title = {
                Text(
                    text = "Add Saved Place",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BentoTextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newPlaceName,
                        onValueChange = { newPlaceName = it },
                        label = { Text("Place Name (e.g. Home, Office)") },
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusNested),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPrimary,
                            unfocusedBorderColor = BentoBorder,
                            focusedLabelColor = BentoPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPlaceAddress,
                        onValueChange = { newPlaceAddress = it },
                        label = { Text("Address / Cross Street") },
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusNested),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPrimary,
                            unfocusedBorderColor = BentoBorder,
                            focusedLabelColor = BentoPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPlaceNote,
                        onValueChange = { newPlaceNote = it },
                        label = { Text("Parking Note (e.g. Permit A)") },
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusNested),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BentoPrimary,
                            unfocusedBorderColor = BentoBorder,
                            focusedLabelColor = BentoPrimary
                        ),
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
                    Text("Save Place", color = BentoPrimaryDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = BentoTextSecondary)
                }
            }
        )
    }
}
