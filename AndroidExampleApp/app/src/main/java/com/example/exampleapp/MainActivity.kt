package com.example.exampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import kotlin.math.min

private val ExpressivePrimary = Color(0xFF6D42FF)
private val ExpressiveSecondary = Color(0xFFFF5C8A)
private val ExpressiveTertiary = Color(0xFF18B7A0)
private val ExpressiveYellow = Color(0xFFFFC247)

private val ExpressiveColors = lightColorScheme(
    primary = ExpressivePrimary,
    onPrimary = Color.White,
    secondary = ExpressiveSecondary,
    tertiary = ExpressiveTertiary,
    background = Color(0xFFFFF9FD),
    surface = Color(0xFFFFF9FD),
    surfaceVariant = Color(0xFFF3EFFF),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = ExpressiveColors,
                typography = Typography(
                    displayLarge = Typography().displayLarge.copy(fontWeight = FontWeight.Black),
                    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            ) {
                ExpressiveApp()
            }
        }
    }
}

@Composable
private fun ExpressiveApp() {
    var count by rememberSaveable { mutableIntStateOf(0) }
    var mood by rememberSaveable { mutableStateOf("Spark") }
    var showBurst by rememberSaveable { mutableStateOf(false) }

    val progress = min(count / 10f, 1f)
    val blobSize by animateDpAsState(targetValue = (170 + count * 5).coerceAtMost(240).dp, label = "blobSize")
    val blobRotation by animateFloatAsState(targetValue = count * 4f, label = "blobRotation")

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(color = ExpressiveColors.background, modifier = Modifier.fillMaxSize()) {}

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("EXPRESSIVE", style = MaterialTheme.typography.labelLarge, color = ExpressivePrimary, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("Play with color.", style = MaterialTheme.typography.headlineLarge)
                    Text("Make the interface feel alive.", color = Color(0xFF665F70))
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(ExpressiveYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF5D4300))
                }
            }

            Card(
                shape = RoundedCornerShape(34.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0EAFF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { mood = "Spark" }, label = { Text("Spark") }, leadingIcon = { Icon(Icons.Default.AutoAwesome, null) })
                        AssistChip(onClick = { mood = "Joy" }, label = { Text("Joy") }, leadingIcon = { Icon(Icons.Default.Celebration, null) })
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Your current vibe", color = Color(0xFF6A6374))
                    Spacer(Modifier.height(4.dp))
                    Text(mood, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(blobSize)
                                .graphicsLayer { rotationZ = blobRotation }
                                .clip(RoundedCornerShape(42.dp, 72.dp, 58.dp, 80.dp))
                                .background(
                                    Brush.linearGradient(listOf(ExpressivePrimary, ExpressiveSecondary, ExpressiveTertiary))
                                )
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$count", style = MaterialTheme.typography.displayLarge, color = Color.White, fontWeight = FontWeight.Black)
                            Text("moments", color = Color.White.copy(alpha = 0.88f))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            count += 1
                            showBurst = true
                        },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpressivePrimary)
                    ) {
                        Icon(Icons.Default.Celebration, null)
                        Spacer(Modifier.width(10.dp))
                        Text("Create a moment", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    AnimatedVisibility(visible = showBurst, enter = fadeIn(), exit = fadeOut()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nice! The interface is responding.", color = ExpressiveTertiary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Text("Choose your energy", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    "Bright" to ExpressiveYellow,
                    "Playful" to ExpressiveSecondary,
                    "Fresh" to ExpressiveTertiary,
                )
                options.forEach { (name, color) ->
                    FilterChip(
                        selected = mood == name,
                        onClick = { mood = name },
                        label = { Text(name) },
                        leadingIcon = { Icon(Icons.Default.Palette, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEEF4)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(ExpressiveSecondary),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Favorite, null, tint = Color.White) }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Expressive by default", fontWeight = FontWeight.ExtraBold)
                        Text("Big shapes, bold type, playful color, and motion.", color = Color(0xFF75646C))
                    }
                    Icon(Icons.Default.ColorLens, null, tint = ExpressiveSecondary)
                }
            }

            Spacer(Modifier.height(88.dp))
        }

        FloatingActionButton(
            onClick = { count = 0; showBurst = false; mood = "Spark" },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = ExpressiveTertiary,
            contentColor = Color.White,
            shape = RoundedCornerShape(22.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset")
        }

        AnimatedVisibility(visible = showBurst, modifier = Modifier.align(Alignment.TopCenter)) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ExpressivePrimary.copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .clickable { showBurst = false }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("✨ Moment +1", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
