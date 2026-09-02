package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CurbLogo
import com.example.ui.theme.CurbBackground
import com.example.ui.theme.CurbBlack
import com.example.ui.theme.CurbOnSurface
import com.example.ui.theme.CurbOnSurfaceVariant
import com.example.ui.theme.CurbWhite

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onTermsClicked: () -> Unit,
    onPrivacyClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CurbWhite)
    ) {
        // FULL SCREEN BACKGROUND IMAGE WITH PRESERVED PROPORTIONS & RATIO
        Image(
            painter = painterResource(id = R.drawable.welcome_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.BottomCenter
        )

        // CONTENT OVERLAY
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP HEADER SECTION
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Medium title text: "Welcome to" (Centered, neutral gray/black typography)
                Text(
                    text = "Welcome to",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = CurbOnSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // App Branding row: Use the app's existing Curb logo vector and bold typography for "CURB"
                CurbLogo(
                    symbolSize = 46.dp,
                    fontSize = 38,
                    showWordmark = true,
                    wordmark = "CURB",
                    tint = CurbBlack
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Subheading text: "Your AI-powered parking assistant for smarter, stress-free parking." (Centered, subtle gray body typography)
                Text(
                    text = "Your AI-powered parking assistant for smarter, stress-free parking.",
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = CurbOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // MIDDLE TRANSPARENT SPACER (allows the full-screen city, car, and parking sign artwork to be visible)
            Spacer(modifier = Modifier.weight(1f))

            // BOTTOM CALL-TO-ACTION SECTION
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Value statement text above button: "Scan a parking sign, get AI-powered insights, and park with confidence."
                Text(
                    text = "Scan a parking sign, get AI-powered insights, and park with confidence.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = CurbOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary CTA Button: Full-width black/dark accent rounded button with white text "Get Started" and a right-arrow icon (Icons.Default.ArrowForward)
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("get_started_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CurbBlack,
                        contentColor = CurbWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CurbWhite
                        )

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Get Started",
                            tint = CurbWhite,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Footer legal text: "By continuing, you agree to our Terms of Service and Privacy Policy." with underlined clickable text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "By continuing, you agree to our ",
                        fontSize = 12.sp,
                        color = CurbOnSurfaceVariant
                    )
                    Text(
                        text = "Terms of Service",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = CurbOnSurface,
                        textDecoration = TextDecoration.Underline,
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
                        fontWeight = FontWeight.Medium,
                        color = CurbOnSurface,
                        textDecoration = TextDecoration.Underline,
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

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

