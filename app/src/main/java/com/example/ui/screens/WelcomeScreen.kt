package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CurbLogo
import com.example.ui.components.CurbPrimaryButton
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbSurfaceVariant

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onContinueAsGuest: () -> Unit = onGetStarted,
    onTermsClicked: () -> Unit,
    onPrivacyClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = CurbOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Official Curb Logo
            CurbLogo(
                symbolSize = 40.dp,
                fontSize = 32,
                showWordmark = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your AI-powered parking assistant for smarter, stress-free parking.",
                fontSize = 18.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Normal,
                color = CurbOnSurface
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Grayscale subtle city visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(CurbSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.welcome_city_1788102283275),
                    contentDescription = "Curb parking illustration",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurbPrimaryButton(
                text = "Get Started",
                onClick = onGetStarted,
                testTag = "get_started_button"
            )

            Spacer(modifier = Modifier.height(10.dp))

            androidx.compose.material3.TextButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.testTag("continue_as_guest_button")
            ) {
                Text(
                    text = "Continue as Guest",
                    color = CurbBlack,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "By continuing, you agree to our ",
                    fontSize = 12.sp,
                    color = CurbOnSurfaceVariant
                )
                Text(
                    text = "Terms",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbBlack,
                    modifier = Modifier
                        .clickable { onTermsClicked() }
                        .testTag("terms_link")
                )
                Text(
                    text = " and ",
                    fontSize = 12.sp,
                    color = CurbOnSurfaceVariant
                )
                Text(
                    text = "Privacy Policy",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CurbBlack,
                    modifier = Modifier
                        .clickable { onPrivacyClicked() }
                        .testTag("privacy_link")
                )
                Text(
                    text = ".",
                    fontSize = 12.sp,
                    color = CurbOnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
