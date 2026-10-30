package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.JarvisAiService
import com.example.audio.JarvisSoundEffects
import com.example.audio.VoiceManager
import com.example.automations.DeviceTelemetry
import com.example.automations.SystemAutomationManager
import com.example.data.db.JarvisDatabase
import com.example.data.db.MemoryEntity
import com.example.data.db.VoiceLogEntity
import com.example.ui.components.AssistantState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.work.BatteryMonitorScheduler
import com.example.work.BatteryMonitorWorker

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = JarvisDatabase.getDatabase(application)
    private val memoryDao = db.memoryDao()
    private val automationManager = SystemAutomationManager(application)
    private val soundEffects = JarvisSoundEffects()
    private val aiService = JarvisAiService()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _userTranscript = MutableStateFlow("")
    val userTranscript: StateFlow<String> = _userTranscript.asStateFlow()

    private val _jarvisSpeechText = MutableStateFlow("All systems online, Sir. Tap the arc core or microphone to initiate voice command.")
    val jarvisSpeechText: StateFlow<String> = _jarvisSpeechText.asStateFlow()

    private val _deviceTelemetry = MutableStateFlow(automationManager.getDeviceTelemetry())
    val deviceTelemetry: StateFlow<DeviceTelemetry> = _deviceTelemetry.asStateFlow()

    private val _isSoundMuted = MutableStateFlow(false)
    val isSoundMuted: StateFlow<Boolean> = _isSoundMuted.asStateFlow()

    private val _isMiniHudActive = MutableStateFlow(false)
    val isMiniHudActive: StateFlow<Boolean> = _isMiniHudActive.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _speechPitch = MutableStateFlow(1.05f)
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _speechRate = MutableStateFlow(1.05f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _isStandbyWakeWordEnabled = MutableStateFlow(true)
    val isStandbyWakeWordEnabled: StateFlow<Boolean> = _isStandbyWakeWordEnabled.asStateFlow()

    private val _visionImage = MutableStateFlow<Bitmap?>(null)
    val visionImage: StateFlow<Bitmap?> = _visionImage.asStateFlow()

    // WorkManager Power-Saving State
    private val _isPowerSavingMode = MutableStateFlow(false)
    val isPowerSavingMode: StateFlow<Boolean> = _isPowerSavingMode.asStateFlow()

    private val _showPowerSaveSuggestion = MutableStateFlow(false)
    val showPowerSaveSuggestion: StateFlow<Boolean> = _showPowerSaveSuggestion.asStateFlow()

    private val _workerBatteryPct = MutableStateFlow(automationManager.getDeviceTelemetry().batteryPct)
    val workerBatteryPct: StateFlow<Int> = _workerBatteryPct.asStateFlow()

    val memories: StateFlow<List<MemoryEntity>> = memoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceLogs: StateFlow<List<VoiceLogEntity>> = memoryDao.getRecentVoiceLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val voiceManager = VoiceManager(
        context = application,
        onSpeechResult = { text ->
            _userTranscript.value = text
            processUserInput(text)
        },
        onError = { errorMsg ->
            _assistantState.value = AssistantState.ERROR
            _jarvisSpeechText.value = errorMsg
            soundEffects.playErrorTone()
        },
        onSpeakingStateChanged = { isSpeaking ->
            if (isSpeaking) {
                _assistantState.value = AssistantState.SPEAKING
            } else if (_assistantState.value == AssistantState.SPEAKING) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    )

    val audioAmplitude: StateFlow<Float> = voiceManager.audioAmplitude

    init {
        refreshTelemetry()
        initWorkManagerBatteryMonitoring()
    }

    private fun initWorkManagerBatteryMonitoring() {
        BatteryMonitorScheduler.schedulePeriodicMonitoring(getApplication())
        BatteryMonitorScheduler.triggerImmediateCheck(getApplication())

        // Observe WorkManager immediate evaluation
        viewModelScope.launch {
            BatteryMonitorScheduler.getImmediateWorkInfosFlow(getApplication()).collect { workInfos ->
                val info = workInfos.firstOrNull()
                if (info != null && info.state.isFinished) {
                    val isLow = info.outputData.getBoolean(BatteryMonitorWorker.KEY_IS_LOW, false)
                    val pct = info.outputData.getInt(BatteryMonitorWorker.KEY_BATTERY_PCT, _deviceTelemetry.value.batteryPct)
                    _workerBatteryPct.value = pct
                    if (isLow && !_isPowerSavingMode.value) {
                        _showPowerSaveSuggestion.value = true
                    }
                }
            }
        }

        // Observe periodic WorkManager monitoring
        viewModelScope.launch {
            BatteryMonitorScheduler.getWorkInfosFlow(getApplication()).collect { workInfos ->
                val info = workInfos.firstOrNull()
                if (info != null && info.state.isFinished) {
                    val isLow = info.outputData.getBoolean(BatteryMonitorWorker.KEY_IS_LOW, false)
                    val pct = info.outputData.getInt(BatteryMonitorWorker.KEY_BATTERY_PCT, _deviceTelemetry.value.batteryPct)
                    _workerBatteryPct.value = pct
                    if (isLow && !_isPowerSavingMode.value) {
                        _showPowerSaveSuggestion.value = true
                    }
                }
            }
        }
    }

    fun refreshTelemetry() {
        _deviceTelemetry.value = automationManager.getDeviceTelemetry()
        _isTorchOn.value = automationManager.isTorchOn
    }

    fun startListening() {
        interruptBargeIn()
        soundEffects.playListeningStart()
        _assistantState.value = AssistantState.LISTENING
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun interruptBargeIn() {
        voiceManager.stopSpeaking()
        voiceManager.stopListening()
        _assistantState.value = AssistantState.IDLE
    }

    fun processUserInput(query: String, imageBitmap: Bitmap? = null) {
        if (query.isBlank() && imageBitmap == null) return

        _assistantState.value = AssistantState.PROCESSING
        _userTranscript.value = query
        soundEffects.playWakeChime()

        viewModelScope.launch(Dispatchers.IO) {
            val memoryStrings = memories.value.map { "${it.key}: ${it.value}" }
            val parsedResponse = aiService.generateResponse(
                prompt = query.ifBlank { "Analyze this visual frame, Sir." },
                contextMemories = memoryStrings,
                imageBitmap = imageBitmap
            )

            // Execute detected system action if any
            var actionExecutedDesc: String? = null
            if (parsedResponse.actionType != null) {
                actionExecutedDesc = parsedResponse.actionType
                executeAutomationAction(parsedResponse.actionType, parsedResponse.actionPayload)
                soundEffects.playActionSuccess()
            }

            _jarvisSpeechText.value = parsedResponse.spokenText

            // Speak response via TTS
            voiceManager.speak(parsedResponse.spokenText)

            // Save to Room DB
            memoryDao.insertVoiceLog(
                VoiceLogEntity(
                    userQuery = query.ifBlank { "[Visual Scan Request]" },
                    assistantResponse = parsedResponse.spokenText,
                    actionExecuted = actionExecutedDesc
                )
            )

            refreshTelemetry()
        }
    }

    private fun executeAutomationAction(actionType: String, payload: String?) {
        when (actionType) {
            "TORCH_ON" -> {
                automationManager.setTorch(true)
                _isTorchOn.value = true
            }
            "TORCH_OFF" -> {
                automationManager.setTorch(false)
                _isTorchOn.value = false
            }
            "WHATSAPP" -> {
                automationManager.openWhatsApp(payload)
            }
            "YOUTUBE" -> {
                automationManager.openYouTube(payload)
            }
            "SEARCH" -> {
                if (!payload.isNullOrBlank()) {
                    automationManager.searchGoogle(payload)
                }
            }
            "CALL" -> {
                automationManager.openDialer(payload)
            }
            "TIMER" -> {
                val sec = payload?.toIntOrNull() ?: 60
                automationManager.setTimer(sec)
            }
            "BATTERY" -> {
                refreshTelemetry()
            }
            "REMEMBER" -> {
                if (!payload.isNullOrBlank()) {
                    val parts = payload.split(":", limit = 2)
                    val k = if (parts.size > 1) parts[0].trim() else "User Note"
                    val v = if (parts.size > 1) parts[1].trim() else payload.trim()
                    viewModelScope.launch {
                        memoryDao.insertMemory(MemoryEntity(key = k, value = v))
                    }
                }
            }
        }
    }

    fun toggleTorch() {
        val newState = !automationManager.isTorchOn
        automationManager.setTorch(newState)
        _isTorchOn.value = newState
        soundEffects.playActionSuccess()
        refreshTelemetry()
    }

    fun toggleSound() {
        val next = !_isSoundMuted.value
        _isSoundMuted.value = next
        soundEffects.isMuted = next
    }

    fun toggleMiniHud() {
        _isMiniHudActive.value = !_isMiniHudActive.value
        soundEffects.playActionSuccess()
    }

    fun setVisionImage(bitmap: Bitmap?) {
        _visionImage.value = bitmap
    }

    fun analyzeVisionImage(bitmap: Bitmap, prompt: String) {
        _visionImage.value = bitmap
        processUserInput(prompt.ifBlank { "What do you see in this visual frame, Sir?" }, bitmap)
    }

    fun updateApiKey(key: String) {
        _customApiKey.value = key
        aiService.userCustomApiKey = key
    }

    fun updateSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        voiceManager.speechPitch = pitch
    }

    fun updateSpeechRate(rate: Float) {
        _speechRate.value = rate
        voiceManager.speechRate = rate
    }

    fun toggleStandbyWakeWord() {
        _isStandbyWakeWordEnabled.value = !_isStandbyWakeWordEnabled.value
    }

    fun saveCustomMemory(key: String, value: String) {
        if (key.isBlank() || value.isBlank()) return
        viewModelScope.launch {
            memoryDao.insertMemory(MemoryEntity(key = key.trim(), value = value.trim()))
            soundEffects.playActionSuccess()
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            memoryDao.deleteMemory(memory)
        }
    }

    fun clearVoiceLogs() {
        viewModelScope.launch {
            memoryDao.clearVoiceLogs()
        }
    }

    fun adjustVolume(deltaPercent: Int) {
        automationManager.adjustVolume(deltaPercent)
        soundEffects.playActionSuccess()
    }

    fun triggerQuickAction(type: String, query: String? = null) {
        when (type) {
            "TORCH" -> toggleTorch()
            "WHATSAPP" -> automationManager.openWhatsApp(query)
            "YOUTUBE" -> automationManager.openYouTube(query)
            "CALL" -> automationManager.openDialer(query)
            "SEARCH" -> if (!query.isNullOrBlank()) automationManager.searchGoogle(query)
            "TIMER" -> automationManager.setTimer(60)
            "BATTERY" -> {
                refreshTelemetry()
                processUserInput("Check battery level")
            }
        }
    }

    fun enablePowerSavingMode() {
        _isPowerSavingMode.value = true
        _showPowerSaveSuggestion.value = false
        soundEffects.playActionSuccess()
        val speech = "Tactical power saving engaged. 3D HUD orb operating in reduced 30 FPS mode to conserve energy, Sir."
        _jarvisSpeechText.value = speech
        voiceManager.speak(speech)
    }

    fun disablePowerSavingMode() {
        _isPowerSavingMode.value = false
        soundEffects.playActionSuccess()
        val speech = "Power saving mode disengaged. Full framerate restored to 3D HUD orb, Sir."
        _jarvisSpeechText.value = speech
        voiceManager.speak(speech)
    }

    fun togglePowerSavingMode() {
        if (_isPowerSavingMode.value) {
            disablePowerSavingMode()
        } else {
            enablePowerSavingMode()
        }
    }

    fun dismissPowerSaveSuggestion() {
        _showPowerSaveSuggestion.value = false
    }

    fun triggerWorkManagerBatteryCheck() {
        BatteryMonitorScheduler.triggerImmediateCheck(getApplication())
        refreshTelemetry()
        soundEffects.playActionSuccess()
    }

    fun simulateLowBatterySuggestion(enable: Boolean) {
        if (enable) {
            _workerBatteryPct.value = 18
            _showPowerSaveSuggestion.value = true
            soundEffects.playWakeChime()
        } else {
            _showPowerSaveSuggestion.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
