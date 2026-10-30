package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automations.DeviceTelemetry
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisMuted
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisWhite

@Composable
fun JarvisTopBar(
    telemetry: DeviceTelemetry,
    state: AssistantState,
    isSoundMuted: Boolean,
    isPowerSavingMode: Boolean = false,
    onTogglePowerSave: () -> Unit = {},
    onToggleSound: () -> Unit,
    onToggleMiniHud: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(JarvisSurfaceDark.copy(alpha = 0.9f))
            .border(1.dp, JarvisBorder, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: JARVIS Identity & Core Telemetry
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    AssistantState.IDLE -> JarvisCyan
                                    AssistantState.LISTENING -> JarvisElectricBlue
                                    AssistantState.PROCESSING -> JarvisGold
                                    AssistantState.SPEAKING -> JarvisGreen
                                    AssistantState.ERROR -> Color.Red
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "J.A.R.V.I.S.",
                        color = JarvisWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "v7.0",
                        color = JarvisCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PWR: ${telemetry.batteryPct}%",
                        color = if (telemetry.batteryPct < 20) JarvisGold else JarvisMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (telemetry.isCharging) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Charging",
                            tint = JarvisGold,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = " • RAM: ${telemetry.availableRamMb}M",
                        color = JarvisMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right: Sound toggle & Mini HUD toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Audio engine status
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(JarvisBorder.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Audio Engine",
                        tint = JarvisCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.name,
                        color = JarvisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(onClick = onTogglePowerSave) {
                    Icon(
                        imageVector = Icons.Default.BatterySaver,
                        contentDescription = "Power Saving Mode",
                        tint = if (isPowerSavingMode) JarvisGold else JarvisMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onToggleSound) {
                    Icon(
                        imageVector = if (isSoundMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Sound FX",
                        tint = if (isSoundMuted) JarvisMuted else JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onToggleMiniHud) {
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = "Mini HUD Mode",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
