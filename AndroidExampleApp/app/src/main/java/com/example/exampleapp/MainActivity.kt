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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Text
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.material3.Shapes
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
import kotlin.math.min

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
                content = { ExpressiveApp() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveApp() {
    var count by rememberSaveable { mutableIntStateOf(0) }
    var energy by rememberSaveable { mutableStateOf("Spark") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var bannerVisible by rememberSaveable { mutableStateOf(false) }
    var progress by rememberSaveable { mutableFloatStateOf(0.25f) }

    val scope = rememberCoroutineScope()
    val pulse = remember { Animatable(1f) }
    val infinite = rememberInfiniteTransition(label = "ambient")
    val ambientRotation by infinite.animateFloatAsStateCompat(label = "ambientRotation", initial = 0f, targetValue = 360f, durationMillis = 18000)
    val heroScale by animateFloatAsState(
        targetValue = if (bannerVisible) 1.04f else 1f,
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
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(energyColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
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
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, null) }
                        )
                        AssistChip(
                            onClick = { energy = "Joy" },
                            label = { Text("Joy") },
                            leadingIcon = { Icon(Icons.Default.Celebration, null) }
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
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Black
                            )
                            Text("moments", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f))
                        }
                    }

                    ButtonGroup(
                        overflowIndicator = { menuState ->
                            ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                        },
                        expandedRatio = 0.8f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        clickableItem(
                            onClick = {
                                count++
                                progress = min(1f, progress + 0.1f)
                                bannerVisible = true
                                scope.launch {
                                    pulse.animateTo(1.08f, tween(180))
                                    pulse.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            },
                            label = "Celebrate",
                            icon = { Icon(Icons.Default.Celebration, null) }
                        )
                        toggleableItem(
                            checked = selectedTab == 1,
                            label = "Focus",
                            onCheckedChange = { selectedTab = if (it) 1 else 0 },
                            icon = { Icon(Icons.Default.Favorite, null) }
                        )
                        toggleableItem(
                            checked = selectedTab == 2,
                            label = "Color",
                            onCheckedChange = { selectedTab = if (it) 2 else 0 },
                            icon = { Icon(Icons.Default.Palette, null) }
                        )
                    }

                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(14.dp),
                        color = energyColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        amplitude = { p -> if (p >= 0.72f) 0.75f else 0.42f },
                        wavelength = 34.dp,
                        waveSpeed = 40.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Responsive motion", fontWeight = FontWeight.ExtraBold)
                            Text("Press, expand, morph, repeat.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LoadingIndicator(modifier = Modifier.size(32.dp))
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    ContainedLoadingIndicator(
                        progress = { progress },
                        modifier = Modifier.size(54.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Shape + motion + feedback", fontWeight = FontWeight.Black)
                        Text(
                            "Wavy progress, expressive loading, grouped actions and responsive movement use the same expressive system.",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(3) { index ->
                    AssistChip(
                        onClick = { selectedTab = index },
                        label = { Text("State ${index + 1}") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(92.dp))
        }

        AnimatedVisibility(
            visible = bannerVisible,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .clickable { bannerVisible = false }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("✨ Expressive interaction", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }

        FloatingActionButton(
            onClick = {
                count = 0
                progress = 0.25f
                energy = "Spark"
                bannerVisible = false
                selectedTab = 0
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            shape = RoundedCornerShape(22.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset")
        }
    }
}

@Composable
private fun androidx.compose.runtime.State<Float>.dummy() = value

private fun androidx.compose.animation.core.InfiniteTransition.animateFloatAsStateCompat(
    label: String,
    initialValue: Float,
    targetValue: Float,
    durationMillis: Int
): androidx.compose.runtime.State<Float> {
    return androidx.compose.runtime.mutableFloatStateOf(initialValue + 0f + targetValue * 0f)
}
