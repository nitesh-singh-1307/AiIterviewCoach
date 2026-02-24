package com.example.aiinterview.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.aiinterviewcoach.presentation.theme.AICoachTheme
import com.aiinterviewcoach.presentation.theme.AccentBlue
import com.aiinterviewcoach.presentation.theme.AccentCyan
import com.aiinterviewcoach.presentation.theme.BgDeep
import com.example.aiinterview.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        var keepOnScreen = true
        splash.setKeepOnScreenCondition { keepOnScreen }
        setContent {
            AICoachTheme {
                SplashScreen(
                    onAnimationComplete = {
                        keepOnScreen = false
                        lifecycleScope.launch {
                            // tiny extra delay so fade-out is visible before transition
                            delay(100)
                            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SplashScreen(onAnimationComplete: () -> Unit) {

    // ── Animation values ───────────────────────────────────────────────────────
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.78f) }
    val taglineAlpha = remember { Animatable(0f) }
    val accentAlpha = remember { Animatable(0f) }
    val screenAlpha = remember { Animatable(1f) }

    val easeOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
    val easeInCubic = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)

    // Ambient glow pulse while visible
    val infiniteT = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteT.animateFloat(
        initialValue = 0.2f, targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            tween(1_400, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        // Phase 1 — fade + scale in
        launch {
            contentAlpha.animateTo(1f, tween(700, easing = easeOutCubic))
        }
        contentScale.animateTo(
            1f,
            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow)
        )

        // Staggered tagline
        delay(200)
        taglineAlpha.animateTo(1f, tween(500, easing = easeOutCubic))
        accentAlpha.animateTo(1f, tween(350, easing = easeOutCubic))

        // Phase 2 — hold for 1.8 s
        delay(1_800)

        // Phase 3 — fade screen out
        screenAlpha.animateTo(0f, tween(700, easing = easeInCubic))
        onAnimationComplete()
    }

    // ── UI ─────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(BgDeep),
        contentAlignment = Alignment.Center
    ) {
        // Radial glow
        Box(
            modifier = Modifier
                .size(280.dp)
                .alpha(glowAlpha * contentAlpha.value)
                .background(
                    Brush.radialGradient(
                        listOf(AccentBlue.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .scale(contentScale.value)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "AI",
                color = Color.White,
                fontSize = 76.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                lineHeight = 76.sp
            )
            Text(
                "Coach",
                color = Color.White,
                fontSize = 76.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                lineHeight = 76.sp
            )

            Spacer(Modifier.height(14.dp))

            // Gradient accent rule
            Box(
                modifier = Modifier
                    .alpha(accentAlpha.value)
                    .width(52.dp)
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(AccentBlue, AccentCyan))
                    )
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Practice · Reflect · Succeed",
                modifier = Modifier.alpha(taglineAlpha.value),
                color = Color(0xFF8B949E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
        }

        // Version stamp
        Text(
            "v1.0",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .alpha(taglineAlpha.value * 0.35f),
            color = Color.White,
            fontSize = 10.sp
        )
    }
}
