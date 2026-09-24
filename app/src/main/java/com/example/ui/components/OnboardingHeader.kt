package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoWhite

@Composable
fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int = 4,
    onBack: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // SITUATIONAL BACK BUTTON
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BentoWhite)
                    .border(1.dp, BentoBorder, CircleShape)
                    .testTag("onboarding_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BentoTextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.size(36.dp))
        }

        // TOP-RIGHT ONBOARDING PROGRESS (4-SEGMENTS SLIDING ANIMATION)
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("onboarding_progress_bar")
        ) {
            for (step in 1..totalSteps) {
                val isActive = step == currentStep
                val targetWidth = if (isActive) 28.dp else 12.dp
                val widthAnimated by animateDpAsState(
                    targetValue = targetWidth,
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    ),
                    label = "segment_width"
                )

                Box(
                    modifier = Modifier
                        .width(widthAnimated)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (isActive) BentoPrimaryDark else BentoBorder
                        )
                )
            }
        }
    }
}
