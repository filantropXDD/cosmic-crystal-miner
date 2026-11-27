package com.cosmicminer.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmicminer.app.model.BossUiState
import com.cosmicminer.app.model.CrystalSkin
import com.cosmicminer.app.model.FloatingNumber
import com.cosmicminer.app.model.SpaceLocation
import com.cosmicminer.app.ui.GameViewModel
import com.cosmicminer.app.ui.theme.CosmicColors
import com.cosmicminer.app.util.NumberFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val MAX_SPARK_PARTICLES = 36
private const val MAX_FLOATING_NUMBERS = 7
private const val MAX_FLOATING_NUMBERS_POTATO = 4

private enum class ParticleStyle {
    SPARK,
    INWARD_VOID,
    MATRIX_BIT,
    SNOWFLAKE
}

private class SparkParticle(
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val color: Color,
    val radiusPx: Float,
    val style: ParticleStyle,
    var progress: Float = 0f
)

private data class BgStar(
    val xFrac: Float,
    val yFrac: Float,
    val sizeDp: Float,
    val depth: Float,
    val alpha: Float,
    val tint: Color
)

@Composable
fun CrystalArena(
    rankLevel: Int,
    overchargeSecondsLeft: Int,
    feverMeter: Float,
    feverSecondsLeft: Int,
    comboCount: Int,
    isCometVisible: Boolean,
    cometSeed: Long,
    visualDroneCount: Int,
    selectedSkin: CrystalSkin,
    selectedLocation: SpaceLocation,
    bossState: BossUiState,
    isVibrationEnabled: Boolean,
    isPotatoMode: Boolean,
    onCrystalTap: () -> GameViewModel.ClickResult,
    onCometTap: () -> Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }
    val shakeX = remember { Animatable(0f) }
    val shakeY = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    val latestVibration by rememberUpdatedState(isVibrationEnabled)
    val latestPotato by rememberUpdatedState(isPotatoMode)
    val latestSkin by rememberUpdatedState(selectedSkin)
    val latestBossActive by rememberUpdatedState(bossState.isActive)
    val latestOnCrystalTap by rememberUpdatedState(onCrystalTap)

    var rainbowHue by remember { mutableFloatStateOf(190f) }
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    // Gyroscope / Accelerometer parallax listener (disabled in Potato Mode)
    DisposableEffect(context, isPotatoMode) {
        if (isPotatoMode) {
            tiltX = 0f
            tiltY = 0f
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val gx = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
                    val gy = ((event.values[1] - 4.5f) / 9.81f).coerceIn(-1f, 1f)
                    val nextX = tiltX * 0.84f + (-gx * 24f) * 0.16f
                    val nextY = tiltY * 0.84f + (gy * 24f) * 0.16f
                    if (abs(nextX - tiltX) > 0.35f || abs(nextY - tiltY) > 0.35f) {
                        tiltX = nextX
                        tiltY = nextY
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            if (sensorManager != null && accelSensor != null) {
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    val floatingNumbers = remember { mutableStateListOf<FloatingNumber>() }
    val sparks = remember { mutableListOf<SparkParticle>() }
    var hasActiveSparks by remember { mutableStateOf(false) }
    var sparkFrameTick by remember { mutableIntStateOf(0) }
    var nextId by remember { mutableLongStateOf(0L) }

    val scratchPath = remember { Path() }
    val scratchPts = remember { FloatArray(18) }

    fun pushFloatingNumber(item: FloatingNumber) {
        val cap = if (latestPotato) MAX_FLOATING_NUMBERS_POTATO else MAX_FLOATING_NUMBERS
        while (floatingNumbers.size >= cap) {
            floatingNumbers.removeAt(0)
        }
        floatingNumbers.add(item)
    }

    LaunchedEffect(isPotatoMode) {
        if (isPotatoMode) {
            sparks.clear()
            hasActiveSparks = false
        }
    }

    LaunchedEffect(hasActiveSparks) {
        if (!hasActiveSparks) return@LaunchedEffect
        var lastFrameNs = 0L
        while (isActive && sparks.isNotEmpty()) {
            withFrameNanos { frameNs ->
                val dtSec = if (lastFrameNs == 0L) {
                    0.016f
                } else {
                    ((frameNs - lastFrameNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
                }
                lastFrameNs = frameNs
                val step = dtSec / 0.40f
                for (i in sparks.size - 1 downTo 0) {
                    val p = sparks[i]
                    p.progress += step
                    if (p.progress >= 1f) {
                        sparks.removeAt(i)
                    }
                }
                sparkFrameTick++
                if (sparks.isEmpty()) {
                    hasActiveSparks = false
                }
            }
        }
    }

    val stars = remember(selectedLocation, isPotatoMode) {
        if (isPotatoMode) {
            emptyList()
        } else {
            val rnd = Random(selectedLocation.ordinal * 997 + 42)
            List(24) {
                val tint = when (selectedLocation) {
                    SpaceLocation.RED_GIANT -> if (it % 3 == 0) Color(0xFFFCA5A5) else Color(0xFFFDE68A)
                    SpaceLocation.ASTEROID_BELT -> if (it % 2 == 0) Color(0xFF99F6E4) else Color(0xFFCBD5E1)
                    SpaceLocation.DEEP_VOID -> if (it % 4 == 0) CosmicColors.NeonCyan else Color.White
                    SpaceLocation.NEBULA -> if (it % 3 == 0) CosmicColors.NeonPurpleBright else Color.White
                }
                BgStar(
                    xFrac = rnd.nextFloat(),
                    yFrac = rnd.nextFloat(),
                    sizeDp = 1.1f + rnd.nextFloat() * 1.8f,
                    depth = 0.35f + rnd.nextFloat() * 1.15f,
                    alpha = 0.25f + rnd.nextFloat() * 0.6f,
                    tint = tint
                )
            }
        }
    }

    val staticZeroState = remember { mutableFloatStateOf(0f) }
    val staticOneState = remember { mutableFloatStateOf(1f) }
    val orbitAngleState: State<Float>
    val counterOrbitAngleState: State<Float>
    val pulseGlowState: State<Float>
    val shootingPhaseState: State<Float>

    if (isPotatoMode) {
        orbitAngleState = staticZeroState
        counterOrbitAngleState = staticZeroState
        pulseGlowState = staticOneState
        shootingPhaseState = staticOneState
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "crystal_ambient")
        orbitAngleState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orbit_angle"
        )
        counterOrbitAngleState = infiniteTransition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "counter_orbit"
        )
        pulseGlowState = infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_glow"
        )
        shootingPhaseState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shooting_star"
        )
    }

    val evolutionStage = when {
        rankLevel >= 150 -> 4
        rankLevel >= 100 -> 3
        rankLevel >= 50 -> 2
        else -> 1
    }
    val isOvercharged = overchargeSecondsLeft > 0
    val isFever = feverSecondsLeft > 0

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isPotatoMode) {
                    Modifier
                } else {
                    Modifier.graphicsLayer {
                        translationX = shakeX.value
                        translationY = shakeY.value
                    }
                }
            )
    ) {
        val arenaWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

        // Parallax Space Location Backdrop (skipped in Potato Mode for zero fillrate overhead)
        if (!isPotatoMode) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val orbitAngle = orbitAngleState.value
                val shootingPhase = shootingPhaseState.value
            when (selectedLocation) {
                SpaceLocation.NEBULA -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF3B0764).copy(alpha = 0.45f), Color.Transparent),
                            center = Offset(size.width * 0.25f + tiltX * 0.6f, size.height * 0.35f + tiltY * 0.6f),
                            radius = size.maxDimension * 0.55f
                        ),
                        radius = size.maxDimension * 0.55f,
                        center = Offset(size.width * 0.25f + tiltX * 0.6f, size.height * 0.35f + tiltY * 0.6f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CosmicColors.NeonCyan.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(size.width * 0.78f - tiltX * 0.5f, size.height * 0.68f - tiltY * 0.5f),
                            radius = size.maxDimension * 0.45f
                        ),
                        radius = size.maxDimension * 0.45f,
                        center = Offset(size.width * 0.78f - tiltX * 0.5f, size.height * 0.68f - tiltY * 0.5f)
                    )
                }
                SpaceLocation.RED_GIANT -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFEA580C).copy(alpha = 0.36f),
                                Color(0xFF7F1D1D).copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.75f + tiltX * 0.7f, size.height * 0.28f + tiltY * 0.7f),
                            radius = size.maxDimension * 0.62f
                        ),
                        radius = size.maxDimension * 0.62f,
                        center = Offset(size.width * 0.75f + tiltX * 0.7f, size.height * 0.28f + tiltY * 0.7f)
                    )
                }
                SpaceLocation.ASTEROID_BELT -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF0F766E).copy(alpha = 0.26f), Color.Transparent),
                            center = Offset(size.width * 0.5f + tiltX * 0.4f, size.height * 0.5f + tiltY * 0.4f),
                            radius = size.maxDimension * 0.65f
                        ),
                        radius = size.maxDimension * 0.65f,
                        center = Offset(size.width * 0.5f + tiltX * 0.4f, size.height * 0.5f + tiltY * 0.4f)
                    )
                    for (i in 0..5) {
                        val drift = (orbitAngle / 360f + i * 0.17f) % 1f
                        val ax = size.width * drift
                        val ay = size.height * (0.14f + (i * 0.14f)) + tiltY * 0.8f
                        drawCircle(
                            color = Color(0xFF64748B).copy(alpha = 0.32f),
                            radius = (5 + (i % 3) * 3).dp.toPx(),
                            center = Offset(ax, ay)
                        )
                    }
                }
                SpaceLocation.DEEP_VOID -> {
                    drawRect(color = Color(0xFF02030A).copy(alpha = 0.55f))
                    if (shootingPhase < 0.32f) {
                        val p = shootingPhase / 0.32f
                        val sx = size.width * (0.15f + p * 0.65f)
                        val sy = size.height * (0.10f + p * 0.45f)
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.85f)),
                                start = Offset(sx - 44.dp.toPx(), sy - 28.dp.toPx()),
                                end = Offset(sx, sy)
                            ),
                            start = Offset(sx - 44.dp.toPx(), sy - 28.dp.toPx()),
                            end = Offset(sx, sy),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

                // Parallax starfield
                stars.forEach { star ->
                    val sx = (star.xFrac * size.width + tiltX * star.depth).coerceIn(0f, size.width)
                    val sy = (star.yFrac * size.height + tiltY * star.depth).coerceIn(0f, size.height)
                    drawCircle(
                        color = star.tint.copy(alpha = star.alpha),
                        radius = star.sizeDp.dp.toPx(),
                        center = Offset(sx, sy)
                    )
                }
            }
        }

        // Top-safe status strip & Boss HP bar (keeps top comet lane separate from central crystal)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bossState.isActive) {
                val hpFrac = (bossState.currentHp / bossState.maxHp.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF450A0A).copy(alpha = 0.82f))
                        .border(1.dp, Color(0xFFF87171).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "БОСС: ${bossState.name}",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${bossState.remainingSeconds}с",
                            color = CosmicColors.NeonGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { hpFrac },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFEF4444),
                        trackColor = Color(0xFF1F2937)
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFever) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                                .border(1.dp, CosmicColors.NeonGold, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ЯРОСТЬ КРИТОВ · ${feverSecondsLeft}с",
                                color = CosmicColors.NeonGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else if (comboCount >= 3) {
                        Text(
                            text = "КОМБО x$comboCount · ${(feverMeter * 100).roundToInt()}%",
                            color = CosmicColors.NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (isOvercharged) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(CosmicColors.NeonGold.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ПЕРЕГРУЗКА x5 · ${overchargeSecondsLeft}с",
                                color = CosmicColors.NeonGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Dedicated Top Comet Lane (strictly above crystal tap zone to avoid accidental taps!)
        if (isCometVisible) {
            key(cometSeed) {
                FlyingComet(
                    arenaWidthPx = arenaWidthPx,
                    isVibrationEnabled = isVibrationEnabled,
                    isPotatoMode = isPotatoMode,
                    onCatch = { x, y ->
                        val bonus = onCometTap()
                        if (bonus > 0.0) {
                            if (isVibrationEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            val id = nextId++
                            pushFloatingNumber(
                                FloatingNumber(
                                    id = id,
                                    text = "КОМЕТА! +${NumberFormatter.format(bonus)} · x5",
                                    x = x.coerceIn(70f, maxOf(80f, arenaWidthPx - 140f)),
                                    y = y,
                                    isCritical = true,
                                    isComet = true
                                )
                            )
                        }
                    }
                )
            }
        }

        // Central Interactive Crystal / Boss Arena (shifted down slightly below top comet lane)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 26.dp)
                .size(196.dp)
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val result = latestOnCrystalTap()
                        val skinNow = latestSkin
                        val potatoNow = latestPotato
                        val bossNow = latestBossActive
                        if (skinNow == CrystalSkin.RAINBOW_PRISM) {
                            rainbowHue = (rainbowHue + 42f) % 360f
                        }

                        if (latestVibration) {
                            haptic.performHapticFeedback(
                                if (result.isCritical) HapticFeedbackType.LongPress
                                else HapticFeedbackType.TextHandleMove
                            )
                        }

                        scope.launch {
                            scaleAnim.snapTo(if (result.isCritical) 0.80f else 0.87f)
                            scaleAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = if (potatoNow) {
                                    tween(durationMillis = 90)
                                } else {
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                }
                            )
                        }

                        if (!potatoNow && result.isCritical) {
                            scope.launch {
                                flashAlpha.snapTo(0.72f)
                                flashAlpha.animateTo(0f, tween(240))
                            }
                            scope.launch {
                                repeat(4) {
                                    shakeX.snapTo(Random.nextFloat() * 12f - 6f)
                                    shakeY.snapTo(Random.nextFloat() * 12f - 6f)
                                    delay(26L)
                                }
                                shakeX.snapTo(0f)
                                shakeY.snapTo(0f)
                            }
                        }

                        if (!potatoNow) {
                            val centerPx = Offset(size.width / 2f, size.height / 2f)
                            val pStyle = when {
                                bossNow -> ParticleStyle.SPARK
                                skinNow == CrystalSkin.BLACK_HOLE -> ParticleStyle.INWARD_VOID
                                skinNow == CrystalSkin.CYBER_CUBE -> ParticleStyle.MATRIX_BIT
                                skinNow == CrystalSkin.ICE_RELIC -> ParticleStyle.SNOWFLAKE
                                else -> ParticleStyle.SPARK
                            }
                            val sparkCount = if (result.isCritical) 10 else 5
                            for (i in 0 until sparkCount) {
                                val angle = (Math.PI * 2.0 * i / sparkCount) + Random.nextDouble() * 0.5
                                val dist = if (result.isCritical) 85f + Random.nextFloat() * 35f else 48f + Random.nextFloat() * 30f
                                val pColor = when {
                                    skinNow == CrystalSkin.CYBER_CUBE -> if (i % 2 == 0) Color(0xFF4ADE80) else Color(0xFF22D3EE)
                                    skinNow == CrystalSkin.ICE_RELIC -> if (i % 2 == 0) Color(0xFFBAE6FD) else Color.White
                                    skinNow == CrystalSkin.SOLAR_CORE -> if (i % 2 == 0) Color(0xFFFDE047) else Color(0xFFF97316)
                                    skinNow == CrystalSkin.RAINBOW_PRISM -> Color.hsv((rainbowHue + i * 32f) % 360f, 0.85f, 1f)
                                    skinNow == CrystalSkin.BLACK_HOLE -> if (i % 2 == 0) CosmicColors.NeonPurpleBright else CosmicColors.NeonGold
                                    result.isCritical -> CosmicColors.NeonGold
                                    i % 2 == 0 -> CosmicColors.NeonCyan
                                    else -> CosmicColors.NeonPurpleBright
                                }
                                val sx = if (pStyle == ParticleStyle.INWARD_VOID) {
                                    centerPx.x + (cos(angle) * dist).toFloat()
                                } else tapOffset.x
                                val sy = if (pStyle == ParticleStyle.INWARD_VOID) {
                                    centerPx.y + (sin(angle) * dist).toFloat()
                                } else tapOffset.y
                                val tx = if (pStyle == ParticleStyle.INWARD_VOID) {
                                    centerPx.x
                                } else tapOffset.x + (cos(angle) * dist).toFloat()
                                val ty = if (pStyle == ParticleStyle.INWARD_VOID) {
                                    centerPx.y
                                } else tapOffset.y + (sin(angle) * dist).toFloat()

                                while (sparks.size >= MAX_SPARK_PARTICLES) {
                                    sparks.removeAt(0)
                                }
                                sparks.add(
                                    SparkParticle(
                                        startX = sx,
                                        startY = sy,
                                        targetX = tx,
                                        targetY = ty,
                                        color = pColor,
                                        radiusPx = if (result.isCritical) 8f else 5.2f,
                                        style = pStyle
                                    )
                                )
                            }
                            if (sparks.isNotEmpty() && !hasActiveSparks) {
                                hasActiveSparks = true
                            }
                        }

                        val id = nextId++
                        val formatted = "+${NumberFormatter.formatRate(result.gained)}"
                        val label = if (result.isCritical) "$formatted КРИТ!" else formatted
                        pushFloatingNumber(
                            FloatingNumber(
                                id = id,
                                text = label,
                                x = tapOffset.x,
                                y = tapOffset.y,
                                isCritical = result.isCritical
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.37f
                val orbitAngle = orbitAngleState.value
                val counterOrbitAngle = counterOrbitAngleState.value
                val pulseGlow = pulseGlowState.value

                // Outer Fever / Combo Heat Arc
                if (feverMeter > 0.01f || isFever) {
                    val feverColor = if (isFever) CosmicColors.NeonGold else CosmicColors.NeonCyan
                    drawArc(
                        color = feverColor.copy(alpha = if (isFever) 0.95f else 0.72f),
                        startAngle = -90f,
                        sweepAngle = 360f * feverMeter.coerceIn(0f, 1f),
                        useCenter = false,
                        topLeft = Offset(center.x - radius * 1.28f, center.y - radius * 1.28f),
                        size = Size(radius * 2.56f, radius * 2.56f),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Ambient Aura
                val auraColor = when {
                    bossState.isActive -> Color(0xFFEF4444)
                    isFever || isOvercharged -> CosmicColors.NeonGold
                    selectedSkin == CrystalSkin.SOLAR_CORE -> Color(0xFFF97316)
                    selectedSkin == CrystalSkin.CYBER_CUBE -> Color(0xFF22C55E)
                    selectedSkin == CrystalSkin.ICE_RELIC -> Color(0xFF38BDF8)
                    selectedSkin == CrystalSkin.RAINBOW_PRISM -> Color.hsv(rainbowHue, 0.85f, 1f)
                    else -> CosmicColors.NeonPurple
                }
                if (isPotatoMode) {
                    drawCircle(
                        color = auraColor.copy(alpha = 0.16f),
                        radius = radius * 1.28f,
                        center = center
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                auraColor.copy(alpha = 0.45f),
                                CosmicColors.NeonCyan.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.5f * pulseGlow
                        ),
                        radius = radius * 1.5f * pulseGlow,
                        center = center
                    )
                }

                // Draw Either Boss Asteroid OR Selected Crystal Skin
                if (bossState.isActive) {
                    drawBossAsteroid(center, radius, orbitAngle, bossState, isPotatoMode, scratchPath, scratchPts)
                } else {
                    when (selectedSkin) {
                        CrystalSkin.CLASSIC -> drawClassicCrystal(center, radius, orbitAngle, counterOrbitAngle, evolutionStage, isOvercharged || isFever, isPotatoMode, scratchPath)
                        CrystalSkin.BLACK_HOLE -> drawBlackHoleSkin(center, radius, orbitAngle, counterOrbitAngle, pulseGlow, isPotatoMode)
                        CrystalSkin.SOLAR_CORE -> drawSolarCoreSkin(center, radius, orbitAngle, pulseGlow, isPotatoMode)
                        CrystalSkin.CYBER_CUBE -> drawCyberCubeSkin(center, radius, orbitAngle, counterOrbitAngle, scratchPts)
                        CrystalSkin.ICE_RELIC -> drawIceRelicSkin(center, radius, orbitAngle, counterOrbitAngle, isPotatoMode, scratchPath)
                        CrystalSkin.RAINBOW_PRISM -> drawRainbowPrismSkin(center, radius, orbitAngle, rainbowHue, isPotatoMode, scratchPath)
                    }
                }

                // Orbiting Fleet Drones + Periodic Laser Shots
                if (visualDroneCount > 0) {
                    val beamTargetIdx = if (isPotatoMode) -1 else ((orbitAngle / 36f).toInt()) % visualDroneCount
                    val beamPulse = if (isPotatoMode) 1f else ((orbitAngle % 36f) / 36f)
                    for (i in 0 until visualDroneCount) {
                        val baseDeg = if (isPotatoMode) {
                            (360f / visualDroneCount) * i
                        } else {
                            orbitAngle * (if (i % 2 == 0) 1.4f else -1.1f) + (360f / visualDroneCount) * i
                        }
                        val rad = Math.toRadians(baseDeg.toDouble())
                        val orbitR = radius * (1.16f + (i % 3) * 0.12f)
                        val dx = center.x + (orbitR * cos(rad)).toFloat()
                        val dy = center.y + (orbitR * 0.72f * sin(rad)).toFloat()
                        val dronePos = Offset(dx, dy)

                        if (i == beamTargetIdx && beamPulse < 0.35f) {
                            drawLine(
                                color = CosmicColors.NeonCyan.copy(alpha = (1f - beamPulse / 0.35f) * 0.85f),
                                start = dronePos,
                                end = center,
                                strokeWidth = 1.8.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        drawCircle(
                            color = if (i % 2 == 0) CosmicColors.NeonCyan else CosmicColors.NeonGold,
                            radius = 3.2.dp.toPx(),
                            center = dronePos
                        )
                    }
                }

                // Critical Flash Overlay
                if (!isPotatoMode && flashAlpha.value > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = flashAlpha.value),
                                CosmicColors.NeonGold.copy(alpha = flashAlpha.value * 0.55f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.4f
                        ),
                        radius = radius * 1.4f,
                        center = center
                    )
                }
            }

            // Pooled tap sparks drawn in a single Canvas
            if (!isPotatoMode && hasActiveSparks) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (sparkFrameTick < 0) return@Canvas
                    for (i in 0 until sparks.size) {
                        val spark = sparks[i]
                        val p = spark.progress.coerceIn(0f, 1f)
                        val curX = spark.startX + (spark.targetX - spark.startX) * p
                        val curY = spark.startY + (spark.targetY - spark.startY) * p
                        val alpha = if (spark.style == ParticleStyle.INWARD_VOID) {
                            (1f - p * 0.7f).coerceIn(0f, 1f)
                        } else {
                            (1f - p).coerceIn(0f, 1f)
                        }

                        when (spark.style) {
                            ParticleStyle.MATRIX_BIT -> {
                                val side = spark.radiusPx * 1.7f
                                drawRect(
                                    color = spark.color.copy(alpha = alpha),
                                    topLeft = Offset(curX - side / 2f, curY - side / 2f),
                                    size = Size(side, side)
                                )
                            }
                            ParticleStyle.SNOWFLAKE -> {
                                val arm = spark.radiusPx * 1.4f
                                for (k in 0 until 3) {
                                    val a = k * (PI / 3.0) + p * 1.5
                                    drawLine(
                                        color = spark.color.copy(alpha = alpha),
                                        start = Offset(
                                            curX - (arm * cos(a)).toFloat(),
                                            curY - (arm * sin(a)).toFloat()
                                        ),
                                        end = Offset(
                                            curX + (arm * cos(a)).toFloat(),
                                            curY + (arm * sin(a)).toFloat()
                                        ),
                                        strokeWidth = 1.6f
                                    )
                                }
                            }
                            else -> {
                                drawCircle(
                                    color = spark.color.copy(alpha = alpha),
                                    radius = spark.radiusPx * (1f - p * 0.35f),
                                    center = Offset(curX, curY)
                                )
                            }
                        }
                    }
                }
            }

            // Floating numbers
            floatingNumbers.forEach { item ->
                key(item.id) {
                    FloatingTapNumber(
                        item = item,
                        onFinished = { floatingNumbers.removeAll { it.id == item.id } }
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawClassicCrystal(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    counterOrbitAngle: Float,
    evolutionStage: Int,
    isGolden: Boolean,
    isPotatoMode: Boolean,
    scratchPath: Path
) {
    if (isPotatoMode) {
        drawCircle(
            color = (if (isGolden) CosmicColors.NeonGold else CosmicColors.NeonCyan).copy(alpha = 0.5f),
            radius = radius * 1.08f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
    } else {
        rotate(degrees = orbitAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        if (isGolden) CosmicColors.NeonGold else CosmicColors.NeonCyan,
                        Color.Transparent,
                        CosmicColors.NeonPurpleBright.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.08f, center.y - radius * 1.08f),
                size = Size(radius * 2.16f, radius * 2.16f),
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        if (evolutionStage >= 2) {
            rotate(degrees = counterOrbitAngle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(CosmicColors.NeonGold.copy(alpha = 0.85f), Color.Transparent, CosmicColors.NeonCyan.copy(alpha = 0.55f), Color.Transparent),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 1.22f, center.y - radius * 0.82f),
                    size = Size(radius * 2.44f, radius * 1.64f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }

    val top = Offset(center.x, center.y - radius * 0.88f)
    val upperRight = Offset(center.x + radius * 0.58f, center.y - radius * 0.28f)
    val lowerRight = Offset(center.x + radius * 0.48f, center.y + radius * 0.45f)
    val bottom = Offset(center.x, center.y + radius * 0.90f)
    val lowerLeft = Offset(center.x - radius * 0.48f, center.y + radius * 0.45f)
    val upperLeft = Offset(center.x - radius * 0.58f, center.y - radius * 0.28f)
    val core = Offset(center.x, center.y - radius * 0.04f)

    scratchPath.reset()
    scratchPath.moveTo(top.x, top.y)
    scratchPath.lineTo(upperRight.x, upperRight.y)
    scratchPath.lineTo(lowerRight.x, lowerRight.y)
    scratchPath.lineTo(bottom.x, bottom.y)
    scratchPath.lineTo(lowerLeft.x, lowerLeft.y)
    scratchPath.lineTo(upperLeft.x, upperLeft.y)
    scratchPath.close()

    if (isPotatoMode) {
        val fillCol = when {
            isGolden -> CosmicColors.NeonGold
            evolutionStage >= 4 -> Color(0xFF4C1D95)
            evolutionStage >= 3 -> Color(0xFF6D28D9)
            else -> CosmicColors.NeonPurple
        }
        drawPath(scratchPath, color = fillCol)
    } else {
        val crystalColors = when {
            isGolden -> listOf(Color(0xFFFEF08A), CosmicColors.NeonGold, Color(0xFF78350F))
            evolutionStage >= 4 -> listOf(Color(0xFF22D3EE), Color(0xFF6D28D9), Color(0xFF090518))
            evolutionStage >= 3 -> listOf(Color(0xFFFDE68A), Color(0xFF8B5CF6), Color(0xFF312E81))
            else -> listOf(Color(0xFF67E8F9), CosmicColors.NeonPurple, Color(0xFF4C1D95))
        }
        drawPath(scratchPath, Brush.linearGradient(crystalColors, top, bottom))
    }
    val facetColor = if (evolutionStage >= 2) CosmicColors.NeonGold.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.35f)
    val facetStroke = 1.5.dp.toPx()
    drawLine(facetColor, top, core, facetStroke)
    drawLine(facetColor, upperLeft, core, facetStroke)
    drawLine(facetColor, upperRight, core, facetStroke)
    drawLine(facetColor, lowerLeft, core, facetStroke)
    drawLine(facetColor, lowerRight, core, facetStroke)
    drawLine(facetColor, bottom, core, facetStroke)
    drawPath(
        scratchPath,
        color = if (isGolden || evolutionStage >= 3) CosmicColors.NeonGold.copy(alpha = 0.9f) else CosmicColors.NeonCyan.copy(alpha = 0.85f),
        style = Stroke(width = 2.4.dp.toPx())
    )
}

private fun DrawScope.drawBlackHoleSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    counterOrbitAngle: Float,
    pulseGlow: Float,
    isPotatoMode: Boolean
) {
    if (isPotatoMode) {
        drawOval(
            color = Color(0xFFF97316).copy(alpha = 0.8f),
            topLeft = Offset(center.x - radius * 1.15f, center.y - radius * 0.48f),
            size = Size(radius * 2.3f, radius * 0.96f),
            style = Stroke(width = 5.dp.toPx())
        )
        drawCircle(color = Color.Black, radius = radius * 0.56f, center = center)
        drawCircle(
            color = Color(0xFFFDBA74).copy(alpha = 0.85f),
            radius = radius * 0.57f,
            center = center,
            style = Stroke(width = 2.2.dp.toPx())
        )
        return
    }
    rotate(degrees = orbitAngle, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(Color(0xFFF97316), Color(0xFF9333EA), Color(0xFFFDE047), Color(0xFF581C87)),
                center = center
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 1.18f, center.y - radius * 0.52f),
            size = Size(radius * 2.36f, radius * 1.04f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
        )
    }
    rotate(degrees = counterOrbitAngle, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(CosmicColors.NeonCyan.copy(alpha = 0.7f), Color.Transparent, Color(0xFFC084FC), Color.Transparent),
                center = center
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 0.95f, center.y - radius * 0.95f),
            size = Size(radius * 1.9f, radius * 1.9f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black, Color.Black, Color(0xFF6B21A8).copy(alpha = 0.85f), Color.Transparent),
            center = center,
            radius = radius * 0.86f * pulseGlow
        ),
        radius = radius * 0.86f * pulseGlow,
        center = center
    )
    drawCircle(
        color = Color.Black,
        radius = radius * 0.56f,
        center = center
    )
    drawCircle(
        color = Color(0xFFFDBA74).copy(alpha = 0.85f),
        radius = radius * 0.57f,
        center = center,
        style = Stroke(width = 2.2.dp.toPx())
    )
}

private fun DrawScope.drawSolarCoreSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    pulseGlow: Float,
    isPotatoMode: Boolean
) {
    if (isPotatoMode) {
        drawCircle(color = Color(0xFFEA580C), radius = radius * 0.78f, center = center)
        drawCircle(color = Color(0xFFFACC15), radius = radius * 0.56f, center = center)
        return
    }
    rotate(degrees = orbitAngle * 0.8f, pivot = center) {
        for (i in 0 until 12) {
            val a = i * (PI * 2.0 / 12.0)
            val start = Offset(center.x + (radius * 0.65f * cos(a)).toFloat(), center.y + (radius * 0.65f * sin(a)).toFloat())
            val end = Offset(center.x + (radius * 1.06f * pulseGlow * cos(a)).toFloat(), center.y + (radius * 1.06f * pulseGlow * sin(a)).toFloat())
            drawLine(
                brush = Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFEA580C).copy(alpha = 0.1f)), start, end),
                start = start,
                end = end,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFEF9C3), Color(0xFFFACC15), Color(0xFFEA580C), Color(0xFF9A3412)),
            center = center,
            radius = radius * 0.78f
        ),
        radius = radius * 0.78f,
        center = center
    )
}

private fun DrawScope.drawCyberCubeSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    counterOrbitAngle: Float,
    scratchPts: FloatArray
) {
    val drawWireCube: (Float, Float, Color) -> Unit = { r, angleDeg, col ->
        val rad = Math.toRadians(angleDeg.toDouble())
        for (i in 0 until 4) {
            val a = rad + i * PI / 2.0
            val cx = center.x + (r * cos(a)).toFloat()
            val sinY = (r * 0.62f * sin(a)).toFloat()
            scratchPts[i * 2] = cx
            scratchPts[i * 2 + 1] = center.y - r * 0.14f + sinY
            scratchPts[8 + i * 2] = cx
            scratchPts[8 + i * 2 + 1] = center.y + r * 0.22f + sinY
        }
        val frontW = 2.2.dp.toPx()
        val backW = 1.6.dp.toPx()
        val linkW = 1.8.dp.toPx()
        val backCol = col.copy(alpha = 0.55f)
        val linkCol = col.copy(alpha = 0.8f)
        for (i in 0 until 4) {
            val next = (i + 1) % 4
            val fx1 = scratchPts[i * 2]
            val fy1 = scratchPts[i * 2 + 1]
            val fx2 = scratchPts[next * 2]
            val fy2 = scratchPts[next * 2 + 1]
            val bx1 = scratchPts[8 + i * 2]
            val by1 = scratchPts[8 + i * 2 + 1]
            val bx2 = scratchPts[8 + next * 2]
            val by2 = scratchPts[8 + next * 2 + 1]
            drawLine(col, Offset(fx1, fy1), Offset(fx2, fy2), strokeWidth = frontW)
            drawLine(backCol, Offset(bx1, by1), Offset(bx2, by2), strokeWidth = backW)
            drawLine(linkCol, Offset(fx1, fy1), Offset(bx1, by1), strokeWidth = linkW)
        }
    }
    drawWireCube(radius * 0.84f, orbitAngle, Color(0xFF4ADE80))
    drawWireCube(radius * 0.44f, counterOrbitAngle * 1.2f, CosmicColors.NeonCyan)
}

private fun DrawScope.drawIceRelicSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    counterOrbitAngle: Float,
    isPotatoMode: Boolean,
    scratchPath: Path
) {
    if (!isPotatoMode) {
        rotate(degrees = orbitAngle * 0.4f, pivot = center) {
            for (i in 0 until 6) {
                val a = i * (PI / 3.0)
                val end = Offset(center.x + (radius * 1.05f * cos(a)).toFloat(), center.y + (radius * 1.05f * sin(a)).toFloat())
                drawLine(Color(0xFFBAE6FD).copy(alpha = 0.55f), center, end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
    val top = Offset(center.x, center.y - radius * 0.90f)
    val right = Offset(center.x + radius * 0.62f, center.y)
    val bottom = Offset(center.x, center.y + radius * 0.90f)
    val left = Offset(center.x - radius * 0.62f, center.y)
    scratchPath.reset()
    scratchPath.moveTo(top.x, top.y)
    scratchPath.lineTo(right.x, right.y)
    scratchPath.lineTo(bottom.x, bottom.y)
    scratchPath.lineTo(left.x, left.y)
    scratchPath.close()
    if (isPotatoMode) {
        drawPath(scratchPath, Color(0xFF38BDF8))
    } else {
        drawPath(scratchPath, Brush.linearGradient(listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF0369A1)), top, bottom))
    }
    drawLine(Color.White.copy(alpha = 0.7f), top, bottom, strokeWidth = 1.8.dp.toPx())
    drawLine(Color.White.copy(alpha = 0.5f), left, right, strokeWidth = 1.6.dp.toPx())
    drawPath(scratchPath, Color(0xFFE0F2FE), style = Stroke(width = 2.4.dp.toPx()))
    if (!isPotatoMode) {
        rotate(degrees = counterOrbitAngle, pivot = center) {
            drawCircle(Color(0xFF7DD3FC).copy(alpha = 0.4f), radius = radius * 1.04f, center = center, style = Stroke(1.5.dp.toPx()))
        }
    }
}

private fun DrawScope.drawRainbowPrismSkin(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    hue: Float,
    isPotatoMode: Boolean,
    scratchPath: Path
) {
    val c1 = Color.hsv(hue % 360f, 0.85f, 1f)
    val c2 = Color.hsv((hue + 110f) % 360f, 0.85f, 1f)
    val c3 = Color.hsv((hue + 220f) % 360f, 0.85f, 0.85f)

    if (!isPotatoMode) {
        rotate(degrees = orbitAngle, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(listOf(c1, c2, c3, c1), center = center),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.1f, center.y - radius * 1.1f),
                size = Size(radius * 2.2f, radius * 2.2f),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }

    val top = Offset(center.x, center.y - radius * 0.88f)
    val ur = Offset(center.x + radius * 0.6f, center.y - radius * 0.15f)
    val lr = Offset(center.x + radius * 0.42f, center.y + radius * 0.55f)
    val bot = Offset(center.x, center.y + radius * 0.88f)
    val ll = Offset(center.x - radius * 0.42f, center.y + radius * 0.55f)
    val ul = Offset(center.x - radius * 0.6f, center.y - radius * 0.15f)
    scratchPath.reset()
    scratchPath.moveTo(top.x, top.y); scratchPath.lineTo(ur.x, ur.y); scratchPath.lineTo(lr.x, lr.y)
    scratchPath.lineTo(bot.x, bot.y); scratchPath.lineTo(ll.x, ll.y); scratchPath.lineTo(ul.x, ul.y); scratchPath.close()
    if (isPotatoMode) {
        drawPath(scratchPath, c1)
    } else {
        drawPath(scratchPath, Brush.linearGradient(listOf(c1, c2, c3), top, bot))
    }
    drawPath(scratchPath, Color.White.copy(alpha = 0.85f), style = Stroke(width = 2.4.dp.toPx()))
}

private fun DrawScope.drawBossAsteroid(
    center: Offset,
    radius: Float,
    orbitAngle: Float,
    bossState: BossUiState,
    isPotatoMode: Boolean,
    scratchPath: Path,
    scratchPts: FloatArray
) {
    val hpFrac = (bossState.currentHp / bossState.maxHp.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    rotate(degrees = if (isPotatoMode) 0f else orbitAngle * 0.35f, pivot = center) {
        for (i in 0 until 9) {
            val a = i * (PI * 2.0 / 9.0)
            val rFactor = if (i % 2 == 0) 0.92f else 0.72f
            scratchPts[i * 2] = center.x + (radius * rFactor * cos(a)).toFloat()
            scratchPts[i * 2 + 1] = center.y + (radius * rFactor * sin(a)).toFloat()
        }
        scratchPath.reset()
        scratchPath.moveTo(scratchPts[0], scratchPts[1])
        for (i in 1 until 9) {
            scratchPath.lineTo(scratchPts[i * 2], scratchPts[i * 2 + 1])
        }
        scratchPath.close()
        if (isPotatoMode) {
            drawPath(path = scratchPath, color = Color(0xFF7F1D1D))
        } else {
            drawPath(
                path = scratchPath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF991B1B), Color(0xFF451A03), Color(0xFF1F2937)),
                    center = center,
                    radius = radius
                )
            )
        }
        drawPath(scratchPath, Color(0xFFF87171), style = Stroke(width = 3.dp.toPx()))

        // Damage glowing cracks when HP drops
        if (hpFrac < 0.75f) {
            drawLine(CosmicColors.NeonGold, center, Offset(scratchPts[2], scratchPts[3]), strokeWidth = 2.2.dp.toPx())
            drawLine(CosmicColors.NeonGold, center, Offset(scratchPts[10], scratchPts[11]), strokeWidth = 2.2.dp.toPx())
        }
        if (hpFrac < 0.4f) {
            drawLine(Color(0xFFEF4444), center, Offset(scratchPts[6], scratchPts[7]), strokeWidth = 2.5.dp.toPx())
            drawLine(Color(0xFFEF4444), center, Offset(scratchPts[14], scratchPts[15]), strokeWidth = 2.5.dp.toPx())
        }
    }
}

@Composable
private fun FlyingComet(
    arenaWidthPx: Float,
    isVibrationEnabled: Boolean,
    isPotatoMode: Boolean,
    onCatch: (Float, Float) -> Unit
) {
    val progress = remember { Animatable(0f) }
    val startFromLeft = remember { Random.nextBoolean() }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6200, easing = LinearEasing)
        )
    }

    val safeTravelWidth = maxOf(220f, arenaWidthPx - 140f)
    val t = progress.value
    val xPx = if (startFromLeft) 16f + t * safeTravelWidth else (safeTravelWidth + 16f) - t * safeTravelWidth
    val yPx = 14f + sin(t * Math.PI).toFloat() * 14f

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
                .size(54.dp)
                .clip(CircleShape)
                .pointerInput(isVibrationEnabled) {
                    detectTapGestures {
                        onCatch(xPx, yPx + 24f)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val tailDir = if (startFromLeft) -1f else 1f

                if (isPotatoMode) {
                    drawLine(
                        color = CosmicColors.NeonGold.copy(alpha = 0.7f),
                        start = c,
                        end = Offset(c.x + tailDir * 30.dp.toPx(), c.y + 3.dp.toPx()),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = CosmicColors.NeonGold, radius = 10.dp.toPx(), center = c)
                    drawCircle(color = Color.White, radius = 6.dp.toPx(), center = c)
                    return@Canvas
                }

                // Outer golden tail
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(CosmicColors.NeonGold, Color(0xFFFB7185).copy(alpha = 0.55f), Color.Transparent),
                        start = c,
                        end = Offset(c.x + tailDir * 38.dp.toPx(), c.y + 4.dp.toPx())
                    ),
                    start = c,
                    end = Offset(c.x + tailDir * 38.dp.toPx(), c.y + 4.dp.toPx()),
                    strokeWidth = 9.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Inner icy cyan plasma streak
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White, CosmicColors.NeonCyan.copy(alpha = 0.6f), Color.Transparent),
                        start = c,
                        end = Offset(c.x + tailDir * 26.dp.toPx(), c.y + 2.dp.toPx())
                    ),
                    start = c,
                    end = Offset(c.x + tailDir * 26.dp.toPx(), c.y + 2.dp.toPx()),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Coma halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CosmicColors.NeonGold.copy(alpha = 0.9f), CosmicColors.NeonCyan.copy(alpha = 0.35f), Color.Transparent),
                        center = c,
                        radius = 20.dp.toPx()
                    ),
                    radius = 20.dp.toPx(),
                    center = c
                )
                drawCircle(color = Color.White, radius = 6.5.dp.toPx(), center = c)
            }
        }
    }
}


@Composable
private fun FloatingTapNumber(
    item: FloatingNumber,
    onFinished: () -> Unit
) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val density = LocalDensity.current

    LaunchedEffect(item.id) {
        launch {
            offsetY.animateTo(
                targetValue = with(density) { -78.dp.toPx() },
                animationSpec = tween(durationMillis = 760)
            )
        }
        launch {
            delay(240L)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 520)
            )
            onFinished()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = item.text,
            color = if (item.isCritical) CosmicColors.NeonGold else CosmicColors.NeonCyan,
            fontSize = if (item.isComet) 15.sp else if (item.isCritical) 18.sp else 15.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (item.x - 26f).roundToInt(),
                        y = (item.y + offsetY.value - 26f).roundToInt()
                    )
                }
                .graphicsLayer { this.alpha = alpha.value }
        )
    }
}
