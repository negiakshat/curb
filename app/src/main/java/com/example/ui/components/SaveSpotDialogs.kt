package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BentoBeige
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusHero
import com.example.ui.theme.RadiusNested

@Composable
fun SavedSpotSuccessDialog(
    placeName: String,
    onViewSavedPlaces: () -> Unit,
    onDone: () -> Unit,
    onDismissRequest: () -> Unit = onDone
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            shape = RoundedCornerShape(RadiusHero),
            colors = CardDefaults.cardColors(containerColor = BentoWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("saved_spot_success_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Check Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(CurbSuccessContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = CurbSuccess,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Title & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Spot saved",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimaryDark,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Curb will remember this parking spot and the sign details you just saved.",
                        fontSize = 13.sp,
                        color = BentoTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Saved Place Name Card
                if (placeName.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusNested))
                            .background(BentoBeige)
                            .border(1.dp, BentoBorder, RoundedCornerShape(RadiusNested))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = placeName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoPrimaryDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Actions: VIEW SAVED PLACES & DONE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CurbPrimaryButton(
                        text = "VIEW SAVED PLACES",
                        onClick = onViewSavedPlaces,
                        backgroundColor = BentoPrimaryDark,
                        contentColor = BentoWhite,
                        testTag = "view_saved_places_after_save_button"
                    )

                    CurbSecondaryButton(
                        text = "DONE",
                        onClick = onDone,
                        backgroundColor = BentoSand,
                        contentColor = BentoPrimaryDark,
                        borderColor = BentoBorder,
                        testTag = "done_saved_spot_button"
                    )
                }
            }
        }
    }
}

@Composable
fun DeleteSavedSpotConfirmationDialog(
    spotName: String = "",
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            shape = RoundedCornerShape(RadiusCard),
            colors = CardDefaults.cardColors(containerColor = BentoWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("delete_saved_spot_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning / Trash Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CurbErrorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = CurbError,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title & Supporting Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Delete this saved spot?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimaryDark,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (spotName.isNotBlank()) "This will remove \"$spotName\" and its stored parking information from Curb." else "This will remove the saved parking spot and its stored parking information from Curb.",
                        fontSize = 13.sp,
                        color = BentoTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CurbSecondaryButton(
                        text = "CANCEL",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = BentoSand,
                        contentColor = BentoPrimaryDark,
                        borderColor = BentoBorder,
                        testTag = "cancel_delete_saved_spot_button"
                    )

                    Button(
                        onClick = onConfirmDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("confirm_delete_saved_spot_button"),
                        shape = RoundedCornerShape(RadiusHero),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CurbError,
                            contentColor = BentoWhite
                        )
                    ) {
                        Text(
                            text = "DELETE SPOT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = BentoWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateSavedSpotDialog(
    onViewSavedSpot: () -> Unit,
    onDone: () -> Unit,
    onDismissRequest: () -> Unit = onDone
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            shape = RoundedCornerShape(RadiusCard),
            colors = CardDefaults.cardColors(containerColor = BentoWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("duplicate_saved_spot_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BentoBeige),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Already saved",
                        tint = BentoPrimaryDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Title & Subtitle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Already saved",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BentoPrimaryDark,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "This parking sign is already saved for this spot.",
                        fontSize = 13.sp,
                        color = BentoTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Actions: VIEW SAVED SPOT & DONE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CurbPrimaryButton(
                        text = "VIEW SAVED SPOT",
                        onClick = onViewSavedSpot,
                        backgroundColor = BentoPrimaryDark,
                        contentColor = BentoWhite,
                        testTag = "view_existing_saved_spot_button"
                    )

                    CurbSecondaryButton(
                        text = "DONE",
                        onClick = onDone,
                        backgroundColor = BentoSand,
                        contentColor = BentoPrimaryDark,
                        borderColor = BentoBorder,
                        testTag = "done_saved_spot_button"
                    )
                }
            }
        }
    }
}
