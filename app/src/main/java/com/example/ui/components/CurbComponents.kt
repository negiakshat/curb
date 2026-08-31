package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ScanVerdict
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoBorderStrong
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.ui.theme.BentoWhite
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbError
import com.example.ui.theme.CurbErrorContainer
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbOutline
import com.example.ui.theme.CurbOutlineVariant
import com.example.ui.theme.CurbSuccess
import com.example.ui.theme.CurbSuccessContainer
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbSurfaceVariant
import com.example.ui.theme.CurbWarning
import com.example.ui.theme.CurbWarningContainer
import com.example.ui.theme.CurbWhite

@Composable
fun CurbPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = BentoPrimary,
    contentColor: Color = BentoWhite,
    leadingIcon: ImageVector? = null,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag(testTag),
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = BentoOutlineDisabled(),
            disabledContentColor = BentoWhite.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = contentColor
            )
        }
    }
}

@Composable
private fun BentoOutlineDisabled(): Color = BentoBorderStrong

@Composable
fun CurbSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = BentoBorder,
    backgroundColor: Color = BentoSand,
    contentColor: Color = BentoTextPrimary,
    leadingIcon: ImageVector? = null,
    testTag: String = "secondary_button"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag(testTag),
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                color = contentColor
            )
        }
    }
}

@Composable
fun CurbCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    backgroundColor: Color = BentoSand,
    borderColor: Color = BentoBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val cardModifier = if (onClick != null) {
        modifier.clip(shape).clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

@Composable
fun BentoPillBadge(
    text: String,
    backgroundColor: Color = BentoPeach,
    textColor: Color = BentoPrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun CurbVerdictBadge(
    verdict: ScanVerdict,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val (bgColor, textColor, icon, label) = when (verdict) {
        ScanVerdict.ALLOWED -> Quad(
            CurbSuccessContainer,
            CurbSuccess,
            Icons.Default.CheckCircle,
            "ALLOWED"
        )
        ScanVerdict.RESTRICTED -> Quad(
            CurbErrorContainer,
            CurbError,
            Icons.Default.Error,
            "RESTRICTED"
        )
        ScanVerdict.AMBIGUOUS -> Quad(
            CurbWarningContainer,
            CurbWarning,
            Icons.Default.Warning,
            "UNCLEAR"
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (large) 14.dp else 10.dp,
                vertical = if (large) 7.dp else 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(if (large) 16.dp else 13.dp)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = if (large) 12.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun CurbSegmentedStatusBar(
    allowedCount: Int,
    restrictedCount: Int,
    ambiguousCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (allowedCount + restrictedCount + ambiguousCount).coerceAtLeast(1)
    val allowedRatio = allowedCount.toFloat() / total
    val restrictedRatio = restrictedCount.toFloat() / total
    val ambiguousRatio = ambiguousCount.toFloat() / total

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (allowedRatio > 0f) {
            Box(
                modifier = Modifier
                    .weight(allowedRatio)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CurbSuccess)
            )
        }
        if (restrictedRatio > 0f) {
            Box(
                modifier = Modifier
                    .weight(restrictedRatio)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CurbError)
            )
        }
        if (ambiguousRatio > 0f) {
            Box(
                modifier = Modifier
                    .weight(ambiguousRatio)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CurbWarning)
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

