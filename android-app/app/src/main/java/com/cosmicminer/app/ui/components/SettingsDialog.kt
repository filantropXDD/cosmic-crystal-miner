package com.cosmicminer.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmicminer.app.ui.theme.CosmicColors

private const val REPO_URL = "https://github.com/filantropXDD/cosmic-crystal-miner-"
private const val GITHUB_ICON_PATH =
    "M12 1.27a11 11 0 0 0-3.48 21.46c.55.09.73-.24.73-.53v-1.85c-3.03.66-3.67-1.45-3.67-1.45-.5-1.26-1.21-1.6-1.21-1.6-.99-.68.07-.67.07-.67 1.09.08 1.67 1.12 1.67 1.12.97 1.66 2.54 1.18 3.16.9.1-.7.38-1.18.69-1.45-2.42-.27-4.96-1.21-4.96-5.4 0-1.19.43-2.17 1.12-2.93-.11-.28-.49-1.39.11-2.89 0 0 .92-.29 3 1.12a10.4 10.4 0 0 1 5.5 0c2.08-1.41 3-1.12 3-1.12.6 1.5.22 2.61.11 2.89.7.76 1.12 1.74 1.12 2.93 0 4.2-2.55 5.13-4.98 5.4.39.34.74 1.01.74 2.03v3.01c0 .29.19.63.74.53A11 11 0 0 0 12 1.27z"

@Composable
fun SettingsDialog(
    isVibrationEnabled: Boolean,
    isSoundEnabled: Boolean,
    totalClicks: Long,
    criticalHits: Long,
    cometsCaught: Long,
    darkMatter: Long,
    onVibrationChange: (Boolean) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onResetConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = CosmicColors.SurfaceCard,
            title = {
                Text(
                    text = "Сбросить весь прогресс?",
                    color = CosmicColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Все кристаллы, Тёмная материя, ранг, улучшения и достижения будут удалены без возможности восстановления.",
                    color = CosmicColors.TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetConfirmed()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text("Сбросить", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Отмена", color = CosmicColors.TextSecondary)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicColors.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Настройки · v1.1.0",
                color = CosmicColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Вибрация при клике",
                            color = CosmicColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Тактильный отклик и тряска при крите",
                            color = CosmicColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isVibrationEnabled,
                        onCheckedChange = onVibrationChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CosmicColors.NeonPurple
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Звуковые эффекты",
                            color = CosmicColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Клик, звонкий крит и покупка",
                            color = CosmicColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isSoundEnabled,
                        onCheckedChange = onSoundChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CosmicColors.NeonCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Кликов: $totalClicks · Критов: $criticalHits",
                    color = CosmicColors.TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Комет поймано: $cometsCaught · Тёмная материя: $darkMatter",
                    color = CosmicColors.TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Clickable GitHub Source Code link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmicColors.SurfaceElevated.copy(alpha = 0.7f))
                        .clickable {
                            try {
                                uriHandler.openUri(REPO_URL)
                            } catch (_: Throwable) {
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GitHubIcon(
                        color = CosmicColors.TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Исходный код",
                        color = CosmicColors.NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = "Открыть GitHub",
                        tint = CosmicColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7F1D1D).copy(alpha = 0.65f),
                        contentColor = Color(0xFFFCA5A5)
                    )
                ) {
                    Text("Сбросить прогресс", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово", color = CosmicColors.NeonCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun GitHubIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    val path = remember {
        PathParser().parsePathString(GITHUB_ICON_PATH).toPath()
    }
    Canvas(modifier = modifier) {
        scale(
            scaleX = size.width / 24f,
            scaleY = size.height / 24f,
            pivot = Offset.Zero
        ) {
            drawPath(path = path, color = color)
        }
    }
}
