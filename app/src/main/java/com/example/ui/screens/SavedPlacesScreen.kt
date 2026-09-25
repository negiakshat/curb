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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Signpost
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CurbNote
import com.example.data.model.SavedPlace
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.components.CurbProFeatureBottomSheet
import com.example.ui.components.SavedPlaceDetailSheet
import com.example.ui.theme.BentoBeige
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoCanvas
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextDark
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusChip
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesScreen(
    savedPlaces: List<SavedPlace>,
    notes: List<CurbNote> = emptyList(),
    isPro: Boolean = false,
    onSavePlace: (SavedPlace, (com.example.data.repository.SavePlaceResult) -> Unit) -> Unit = { place, callback ->
        callback(com.example.data.repository.SavePlaceResult.Success(place.copy(id = 1L)))
    },
    onDeletePlace: (Long) -> Unit,
    onCheckSignAgain: (SavedPlace) -> Unit,
    onOpenScan: () -> Unit,
    onUpgradeToPro: () -> Unit = {},
    onBack: () -> Unit
) {
    var selectedDetailPlace by remember { mutableStateOf<SavedPlace?>(null) }
    var spotToDelete by remember { mutableStateOf<SavedPlace?>(null) }
    var spotToEdit by remember { mutableStateOf<SavedPlace?>(null) }
    var spotSavedForSuccessDialog by remember { mutableStateOf<SavedPlace?>(null) }
    var duplicateSavedSpot by remember { mutableStateOf<SavedPlace?>(null) }
    var showProSheet by remember { mutableStateOf(false) }

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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                            text = "Saved parking intelligence & rule schedules",
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
                                    imageVector = Icons.Default.Signpost,
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
                                text = "Scan a parking sign to save real parking intelligence, rule schedules, and location context.",
                                fontSize = 13.sp,
                                color = BentoTextSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            CurbPrimaryButton(
                                text = "SCAN TO SAVE",
                                onClick = onOpenScan,
                                backgroundColor = BentoPrimaryDark,
                                contentColor = BentoWhite,
                                leadingIcon = Icons.Default.CameraAlt,
                                testTag = "scan_to_save_button"
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(savedPlaces, key = { it.id }) { place ->
                        val lastCheckedFormatted = remember(place.lastCheckedAt) {
                            val time = if (place.lastCheckedAt > 0) place.lastCheckedAt else place.timestamp
                            val sdf = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
                            sdf.format(Date(time))
                        }

                        Card(
                            shape = RoundedCornerShape(RadiusCard),
                            colors = CardDefaults.cardColors(containerColor = BentoWhite),
                            border = BorderStroke(1.dp, BentoBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDetailPlace = place }
                                .testTag("saved_place_card_${place.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // TOP ROW: THUMBNAIL / ICON + NAME + DELETE
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (place.signImageUri.isNotBlank()) {
                                            AsyncImage(
                                                model = place.signImageUri,
                                                contentDescription = "Sign Thumbnail",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .border(1.dp, BentoBorder, RoundedCornerShape(10.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(BentoSand)
                                                    .border(1.dp, BentoBorder, RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = BentoPrimaryDark,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = place.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = place.address.ifBlank { "Recorded spot" },
                                                fontSize = 12.sp,
                                                color = BentoTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { spotToDelete = place },
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

                                // INTELLIGENCE SUMMARY & SCHEDULE
                                Surface(
                                    shape = RoundedCornerShape(RadiusNested),
                                    color = BentoBeige,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = place.parkingRuleSummary.ifBlank { "Sign intelligence verified" },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BentoTextDark,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (place.parkingSchedule.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = CurbSuccess,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = place.parkingSchedule,
                                                    fontSize = 11.sp,
                                                    color = CurbSuccess,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Last checked · $lastCheckedFormatted",
                                                fontSize = 10.sp,
                                                color = BentoTextSecondary
                                            )

                                            if (place.reminderEnabled) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsActive,
                                                        contentDescription = null,
                                                        tint = CurbSuccess,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text(
                                                        text = "Reminder ${place.reminderMinutesBefore}m",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = CurbSuccess
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (place.parkingNote.isNotBlank()) {
                                    Text(
                                        text = "Note: ${place.parkingNote}",
                                        fontSize = 12.sp,
                                        color = BentoTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // CHECK SIGN AGAIN BUTTON
                                Surface(
                                    shape = RoundedCornerShape(RadiusChip),
                                    color = BentoSand,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onCheckSignAgain(place) }
                                        .testTag("check_rules_${place.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = BentoPrimaryDark,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CHECK SIGN AGAIN",
                                            fontSize = 12.sp,
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

        // FLOATING ACTION BUTTON
        if (savedPlaces.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(RadiusHero),
                color = BentoPrimaryDark,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .clickable {
                        if (!isPro && savedPlaces.size >= 3) {
                            showProSheet = true
                        } else {
                            onOpenScan()
                        }
                    }
                    .testTag("scan_to_save_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan New Spot",
                        tint = BentoWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "SCAN NEW SPOT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = BentoWhite
                    )
                }
            }
        }
    }

    // DETAIL SHEET FOR TAP ON SPOT CARD
    selectedDetailPlace?.let { place ->
        SavedPlaceDetailSheet(
            place = place,
            onDismiss = { selectedDetailPlace = null },
            onCheckSignAgain = { targetPlace ->
                selectedDetailPlace = null
                onCheckSignAgain(targetPlace)
            },
            onEditSpot = { targetPlace ->
                selectedDetailPlace = null
                spotToEdit = targetPlace
            },
            onDelete = { _ ->
                selectedDetailPlace = null
                spotToDelete = place
            }
        )
    }

    spotToDelete?.let { place ->
        com.example.ui.components.DeleteSavedSpotConfirmationDialog(
            spotName = place.name,
            onConfirmDelete = {
                val targetId = place.id
                spotToDelete = null
                onDeletePlace(targetId)
            },
            onDismiss = {
                spotToDelete = null
            }
        )
    }

    spotToEdit?.let { place ->
        val reconstructedScan = remember(place) {
            com.example.data.model.ScanResult(
                id = place.scanResultId ?: 0L,
                locationName = place.address.ifBlank { place.name },
                parkingRules = if (place.parkingRuleSummary.isNotBlank()) listOf(place.parkingRuleSummary) else emptyList(),
                allowedUntilTime = place.parkingSchedule,
                verdict = try { com.example.data.model.ScanVerdict.valueOf(place.parkingVerdict) } catch (e: Exception) { com.example.data.model.ScanVerdict.ALLOWED },
                imageUri = place.signImageUri
            )
        }
        com.example.ui.components.SaveSpotBottomSheet(
            scanResult = reconstructedScan,
            rescanTargetPlace = place,
            onDismiss = { spotToEdit = null },
            onSave = { updatedPlace ->
                onSavePlace(updatedPlace) { result ->
                    when (result) {
                        is com.example.data.repository.SavePlaceResult.Success -> {
                            spotSavedForSuccessDialog = result.savedPlace
                        }
                        is com.example.data.repository.SavePlaceResult.Duplicate -> {
                            duplicateSavedSpot = result.existingPlace
                        }
                        is com.example.data.repository.SavePlaceResult.Error -> {
                            // error
                        }
                    }
                }
                spotToEdit = null
            }
        )
    }

    spotSavedForSuccessDialog?.let { savedSpot ->
        com.example.ui.components.SavedSpotSuccessDialog(
            placeName = savedSpot.name,
            onViewSavedPlaces = {
                spotSavedForSuccessDialog = null
            },
            onDone = {
                spotSavedForSuccessDialog = null
            }
        )
    }

    duplicateSavedSpot?.let { existingSpot ->
        com.example.ui.components.DuplicateSavedSpotDialog(
            onViewSavedSpot = {
                duplicateSavedSpot = null
                selectedDetailPlace = existingSpot
            },
            onDone = {
                duplicateSavedSpot = null
            }
        )
    }

    if (showProSheet) {
        CurbProFeatureBottomSheet(
            title = "Unlock Unlimited Saved Spots",
            supportingText = "Save as many parking spots and sign rule schedules as you need with Curb Pro.",
            icon = Icons.Default.Signpost,
            onGetPro = onUpgradeToPro,
            onDismiss = { showProSheet = false }
        )
    }
}
