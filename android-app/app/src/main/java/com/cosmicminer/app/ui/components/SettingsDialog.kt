package com.cosmicminer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmicminer.app.ui.theme.CosmicColors

@Composable
fun SettingsDialog(
    isVibrationEnabled: Boolean,
    totalClicks: Long,
    criticalHits: Long,
    onVibrationChange: (Boolean) -> Unit,
    onResetConfirmed: () -> Unit,
    onDismiss: () -> Unit
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = CosmicColors.SurfaceCard,
            title = {
                Text(
                    text = "Сбросить прогресс?",
                    color = CosmicColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Все кристаллы, ранг, улучшения и достижения будут удалены без возможности восстановления.",
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
                text = "Настройки",
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
                            text = "Тактильный отклик при добыче",
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Всего кликов: $totalClicks · Критов: $criticalHits",
                    color = CosmicColors.TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

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
