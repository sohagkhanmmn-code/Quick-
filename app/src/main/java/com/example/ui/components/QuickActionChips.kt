package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisWhite

data class CommandPrompt(
    val label: String,
    val icon: ImageVector,
    val prompt: String
)

val defaultCommands = listOf(
    CommandPrompt("Status Report", Icons.Default.Sensors, "Jarvis, give me a full system status report"),
    CommandPrompt("Turn on Torch", Icons.Default.FlashlightOn, "Jarvis, turn on flashlight"),
    CommandPrompt("Play YouTube", Icons.Default.PlayCircle, "Jarvis, play Iron Man AC DC on YouTube"),
    CommandPrompt("WhatsApp", Icons.AutoMirrored.Filled.Message, "Jarvis, open WhatsApp message"),
    CommandPrompt("Battery Check", Icons.Default.BatteryChargingFull, "Jarvis, what is the battery level?"),
    CommandPrompt("Save Memory", Icons.Default.Bookmark, "Jarvis, remember that meeting is at 4 PM"),
    CommandPrompt("Identity", Icons.Default.Info, "Jarvis, what is your operational protocol?")
)

@Composable
fun QuickActionChips(
    onCommandSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        defaultCommands.forEach { cmd ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(JarvisSurfaceDark)
                    .border(1.dp, JarvisBorder, RoundedCornerShape(14.dp))
                    .clickable { onCommandSelected(cmd.prompt) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = cmd.icon,
                    contentDescription = cmd.label,
                    tint = JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = cmd.label,
                    color = JarvisWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
