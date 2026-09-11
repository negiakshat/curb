package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BentoBorder
import com.example.ui.theme.BentoPeach
import com.example.ui.theme.BentoPrimaryDark
import com.example.ui.theme.BentoSand
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurface
import com.example.ui.theme.CurbWhite
import com.example.ui.theme.RadiusCard
import com.example.ui.theme.RadiusNested
import kotlin.random.Random

private data class ConfettiParticle(
    val initialXRatio: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val driftFactor: Float,
    val rotationSpeed: Float,
    val initialDelayRatio: Float,
    val isCircle: Boolean
)

@Composable
fun ConfettiAnimation(
    modifier: Modifier = Modifier
) {
    val particles = remember {
        val colors = listOf(
            BentoPeach,
            BentoPrimaryDark,
            BentoSand,
            Color(0xFFE9C46A), // Warm gold
            Color(0xFF2A9D8F), // Mint green
            Color(0xFFE76F51), // Coral
            Color(0xFFF4A261), // Ochre
            CurbWhite
        )
        val rng = Random(42)
        List(65) {
            ConfettiParticle(
                initialXRatio = rng.nextFloat(),
                speed = 0.6f + rng.nextFloat() * 0.8f,
                size = 10f + rng.nextFloat() * 14f,
                color = colors[rng.nextInt(colors.size)],
                driftFactor = (rng.nextFloat() - 0.5f) * 120f,
                rotationSpeed = (rng.nextFloat() - 0.5f) * 720f,
                initialDelayRatio = rng.nextFloat() * 0.5f,
                isCircle = rng.nextBoolean()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "confetti")
    val animProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        particles.forEach { particle ->
            val particleProgress = (animProgress + particle.initialDelayRatio) % 1f
            val yPos = particleProgress * (canvasHeight + 100f) - 50f
            val xPos = (particle.initialXRatio * canvasWidth) +
                    (Math.sin((particleProgress * Math.PI * 3).toDouble()).toFloat() * particle.driftFactor)
            val currentRotation = particleProgress * particle.rotationSpeed

            rotate(degrees = currentRotation, pivot = Offset(xPos, yPos)) {
                if (particle.isCircle) {
                    drawCircle(
                        color = particle.color.copy(alpha = (1f - particleProgress * 0.3f).coerceIn(0f, 1f)),
                        radius = particle.size / 2f,
                        center = Offset(xPos, yPos)
                    )
                } else {
                    drawRect(
                        color = particle.color.copy(alpha = (1f - particleProgress * 0.3f).coerceIn(0f, 1f)),
                        topLeft = Offset(xPos - particle.size / 2f, yPos - particle.size / 3f),
                        size = Size(particle.size, particle.size * 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
fun CurbProSuccessDialog(
    isJudgeCode: Boolean = false,
    isRestore: Boolean = false,
    onContinue: () -> Unit
) {
    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .testTag("curb_pro_success_dialog"),
            contentAlignment = Alignment.Center
        ) {
            // CONFETTI CELEBRATION
            ConfettiAnimation(modifier = Modifier.fillMaxSize())

            // DIALOG CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 420.dp)
                    .padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(RadiusCard),
                colors = CardDefaults.cardColors(containerColor = CurbSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // CELEBRATION ICON BADGE
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BentoPrimaryDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Success",
                            tint = BentoPeach,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "🎉",
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val titleText = when {
                        isJudgeCode -> "Judge Demo Unlocked!"
                        isRestore -> "Purchases Restored!"
                        else -> "Curb Pro Unlocked!"
                    }

                    Text(
                        text = titleText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = CurbOnSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val subtitleText = when {
                        isJudgeCode -> "Demo access enabled via code CURB26X.\nEnjoy full Curb Pro features for evaluation."
                        isRestore -> "Welcome back!\nYour Curb Pro subscription has been restored."
                        else -> "Thank you for subscribing!\nEnjoy your Curb Pro subscription."
                    }

                    Text(
                        text = subtitleText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = CurbOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // BENEFIT SUMMARY BOX
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(RadiusNested),
                        color = BentoSand.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BentoBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SuccessBenefitRow(text = "Unlimited AI photo scans")
                            SuccessBenefitRow(text = "Full scan history & export")
                            SuccessBenefitRow(text = "Unlimited saved places")
                            SuccessBenefitRow(text = "Pro tools & real-time AI assistance")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    CurbPrimaryButton(
                        text = "Continue",
                        onClick = onContinue,
                        testTag = "success_continue_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessBenefitRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(BentoPrimaryDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CurbWhite,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = CurbOnSurface
        )
    }
}
