package com.example.exampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private val ExpressivePrimary = Color(0xFF6D42FF)
private val ExpressiveSecondary = Color(0xFFE94772)
private val ExpressiveTertiary = Color(0xFF006C5B)
private val ExpressiveWarm = Color(0xFFFFB74D)
private val ExpressiveSurface = Color(0xFFFFF9FD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialExpressiveTheme(
                colorScheme = expressiveLightColorScheme(),
                motionScheme = MotionScheme.expressive(),
                shapes = Shapes(largeIncreased = RoundedCornerShape(36.dp)),
            ) {
                ExpressiveApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveApp() {
    var count by rememberSaveable { mutableIntStateOf(0) }
    var energy by rememberSaveable { mutableStateOf("Spark") }
    var bannerVisible by rememberSaveable { mutableStateOf(false) }
    var progress by rememberSaveable { mutableFloatStateOf(0.25f) }

    val scope = rememberCoroutineScope()
    val pulse = remember { Animatable(1f) }
    val infinite = rememberInfiniteTransition(label = "ambient")
    val ambientRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000)),
        label = "ambientRotation"
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600)),
        label = "wavePhase"
    )
    val heroScale by animateFloatAsState(
        targetValue = if (bannerVisible) 1.035f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "heroScale"
    )
    val heroSize by animateDpAsState(
        targetValue = (176 + count * 4).coerceAtMost(242).dp,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "heroSize"
    )
    val energyColor by animateColorAsState(
        targetValue = when (energy) {
            "Bright" -> ExpressiveWarm
            "Playful" -> ExpressiveSecondary
            "Fresh" -> Color(0xFF2AAE9B)
            else -> ExpressivePrimary
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "energyColor"
    )

    Box(modifier = Modifier.fillMaxSize().background(ExpressiveSurface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "MATERIAL 3 EXPRESSIVE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.8.sp
                    )
                    AnimatedContent(targetState = energy, label = "headline") { target ->
                        Text(
                            text = when (target) {
                                "Bright" -> "Make it glow."
                                "Playful" -> "Make it playful."
                                "Fresh" -> "Make it breathe."
                                else -> "Make it move."
                            },
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Text(
                        "Expressive color, shape and motion—tied together by one system.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExpressiveGlyph("✦", energyColor, modifier = Modifier.size(56.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = heroScale, scaleY = heroScale),
                shape = RoundedCornerShape(36.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = { energy = "Spark" },
                            label = { Text("Spark") },
                            leadingIcon = { Text("✦", fontSize = 16.sp) }
                        )
                        AssistChip(
                            onClick = { energy = "Joy" },
                            label = { Text("Joy") },
                            leadingIcon = { Text("✺", fontSize = 16.sp) }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(heroSize)
                                .graphicsLayer {
                                    rotationZ = ambientRotation * 0.04f
                                    scaleX = pulse.value
                                    scaleY = pulse.value
                                }
                                .clip(RoundedCornerShape(54.dp, 82.dp, 58.dp, 78.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            energyColor,
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$count",
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                            Text("moments", color = Color.White.copy(alpha = 0.84f))
                        }
                    }

                    Button(
                        onClick = {
                            count++
                            progress = min(1f, progress + 0.1f)
                            bannerVisible = true
                            scope.launch {
                                pulse.animateTo(1.08f, tween(180))
                                pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = energyColor,
                            contentColor = Color.White
                        )
                    ) {
                        Text("✺", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("Create a moment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }

                    ExpressiveWaveProgress(
                        progress = progress,
                        phase = wavePhase,
                        color = energyColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Responsive motion", fontWeight = FontWeight.ExtraBold)
                            Text("Press, pulse, wave, repeat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ExpressiveGlyph("✦", MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(34.dp), glyphColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    AnimatedVisibility(visible = bannerVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { bannerVisible = false }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✨ Expressive interaction",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text("Pick your energy", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Bright", "Playful", "Fresh").forEach { name ->
                    FilterChip(
                        selected = energy == name,
                        onClick = { energy = name },
                        label = { Text(name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveGlyph("♥", MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(56.dp), glyphColor = MaterialTheme.colorScheme.onTertiary)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Expressive by default", fontWeight = FontWeight.Black)
                        Text(
                            "Big type, broad shapes, dynamic color, playful motion and strong interaction feedback.",
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(92.dp))
        }

        FloatingActionButton(
            onClick = {
                count = 0
                progress = 0.25f
                energy = "Spark"
                bannerVisible = false
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("↻", fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ExpressiveGlyph(
    glyph: String,
    containerColor: Color,
    modifier: Modifier,
    glyphColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Text(glyph, color = glyphColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ExpressiveWaveProgress(
    progress: Float,
    phase: Float,
    color: Color,
    trackColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(28) { index ->
            val normalized = index / 27f
            val active = normalized <= progress
            val wave = (sin((normalized * PI * 4.0) + (phase * PI * 2.0)) * 0.5 + 0.5).toFloat()
            val height = if (active) (7 + wave * 9).dp else 5.dp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height)
                    .clip(RoundedCornerShape(50))
                    .background(if (active) color else trackColor)
            )
        }
    }
}
