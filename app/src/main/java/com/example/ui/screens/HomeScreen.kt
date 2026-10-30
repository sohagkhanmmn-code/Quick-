package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.JarvisViewModel
import com.example.ui.components.AssistantState
import com.example.ui.components.JarvisOrb
import com.example.ui.components.JarvisTopBar
import com.example.ui.components.PowerSaveSuggestionBanner
import com.example.ui.components.QuickActionChips
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisMuted
import com.example.ui.theme.JarvisObsidianDark
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSurfaceDark
import com.example.ui.theme.JarvisSurfaceElevated
import com.example.ui.theme.JarvisWhite

@Composable
fun HomeScreen(
    viewModel: JarvisViewModel,
    onRequestRecordPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.assistantState.collectAsState()
    val audioAmp by viewModel.audioAmplitude.collectAsState()
    val transcript by viewModel.userTranscript.collectAsState()
    val jarvisSpeech by viewModel.jarvisSpeechText.collectAsState()
    val telemetry by viewModel.deviceTelemetry.collectAsState()
    val isSoundMuted by viewModel.isSoundMuted.collectAsState()
    val isPowerSavingMode by viewModel.isPowerSavingMode.collectAsState()
    val showPowerSaveSuggestion by viewModel.showPowerSaveSuggestion.collectAsState()
    val workerBatteryPct by viewModel.workerBatteryPct.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisObsidianDark)
    ) {
        // Top HUD Header
        JarvisTopBar(
            telemetry = telemetry,
            state = state,
            isSoundMuted = isSoundMuted,
            isPowerSavingMode = isPowerSavingMode,
            onTogglePowerSave = { viewModel.togglePowerSavingMode() },
            onToggleSound = { viewModel.toggleSound() },
            onToggleMiniHud = { viewModel.toggleMiniHud() }
        )

        // WorkManager Low Battery Power-Saving Suggestion Banner
        PowerSaveSuggestionBanner(
            isVisible = showPowerSaveSuggestion,
            batteryPct = workerBatteryPct,
            isPowerSavingMode = isPowerSavingMode,
            onEnablePowerSave = { viewModel.enablePowerSavingMode() },
            onDismiss = { viewModel.dismissPowerSaveSuggestion() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Attribution & Status Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisSurfaceDark.copy(alpha = 0.6f))
                    .border(1.dp, JarvisBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Jarvis Core Online",
                        tint = JarvisCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "JARVIS NEURAL CORE • SYSTEM ONLINE",
                        color = JarvisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "STATUS: ${state.name}",
                    color = when (state) {
                        AssistantState.IDLE -> JarvisMuted
                        AssistantState.LISTENING -> JarvisElectricBlue
                        AssistantState.PROCESSING -> JarvisGold
                        AssistantState.SPEAKING -> JarvisGreen
                        AssistantState.ERROR -> JarvisRed
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Central 3D Interactive Glowing HUD Orb (Part 1) with Power Saving Mode Support
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                JarvisOrb(
                    state = state,
                    audioAmplitude = audioAmp,
                    size = 250.dp,
                    isPowerSavingMode = isPowerSavingMode,
                    onClick = {
                        if (state == AssistantState.SPEAKING) {
                            viewModel.interruptBargeIn()
                        } else if (state == AssistantState.LISTENING) {
                            viewModel.stopListening()
                        } else {
                            onRequestRecordPermission()
                        }
                    }
                )
            }

            // Audio Level Reactive Visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(JarvisSurfaceDark),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) audioAmp.coerceAtLeast(0.08f) else 0.05f)
                        .background(
                            when (state) {
                                AssistantState.IDLE -> JarvisCyan.copy(alpha = 0.3f)
                                AssistantState.LISTENING -> JarvisElectricBlue
                                AssistantState.PROCESSING -> JarvisGold
                                AssistantState.SPEAKING -> JarvisGreen
                                AssistantState.ERROR -> JarvisRed
                            }
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Voice Interaction HUD Display Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(JarvisSurfaceDark)
                    .border(1.dp, JarvisBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                // User transcription if present
                AnimatedVisibility(visible = transcript.isNotBlank()) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "USER VOICE INPUT:",
                                color = JarvisMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "\"$transcript\"",
                            color = JarvisCyan,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Jarvis Response text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "J.A.R.V.I.S. AUDITORY SYNTHESIS:",
                        color = JarvisCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    if (state == AssistantState.SPEAKING) {
                        Text(
                            text = "TAP TO INTERRUPT",
                            color = JarvisRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.clickable { viewModel.interruptBargeIn() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = jarvisSpeech,
                    color = JarvisWhite,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Command Chips
            Text(
                text = "SYSTEM VOICE MACROS",
                color = JarvisMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 4.dp)
            )

            QuickActionChips(
                onCommandSelected = { prompt ->
                    viewModel.processUserInput(prompt)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Bottom Input Action Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(JarvisSurfaceElevated)
                .border(1.dp, JarvisBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Text input field for silent mode
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            "Type command or speak...",
                            color = JarvisMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = JarvisSurfaceDark,
                        unfocusedContainerColor = JarvisSurfaceDark,
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = JarvisWhite,
                        unfocusedTextColor = JarvisWhite
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textInput.isNotBlank()) {
                            viewModel.processUserInput(textInput)
                            textInput = ""
                        }
                    }),
                    singleLine = true
                )

                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            viewModel.processUserInput(textInput)
                            textInput = ""
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(JarvisCyan)
                            .testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Command",
                            tint = JarvisObsidianDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Barge-In Stop button if speaking
                    if (state == AssistantState.SPEAKING) {
                        IconButton(
                            onClick = { viewModel.interruptBargeIn() },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(JarvisRed)
                                .testTag("barge_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Speech",
                                tint = JarvisWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        // Microphone Trigger
                        IconButton(
                            onClick = {
                                if (state == AssistantState.LISTENING) {
                                    viewModel.stopListening()
                                } else {
                                    onRequestRecordPermission()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (state == AssistantState.LISTENING) JarvisElectricBlue else JarvisCyan)
                                .testTag("voice_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = JarvisObsidianDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
