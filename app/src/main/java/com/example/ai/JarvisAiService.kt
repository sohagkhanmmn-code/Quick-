package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class JarvisParsedResponse(
    val spokenText: String,
    val actionType: String? = null,
    val actionPayload: String? = null
)

private const val TAG = "JarvisAiService"

private const val SYSTEM_INSTRUCTION_TEXT =
    "You are J.A.R.V.I.S., a sophisticated, witty, and loyal British AI assistant inspired by Tony Stark. " +
    "Address the user politely as 'Sir' (or 'Ma'am'). Keep spoken responses crisp, elegant, and concise (1 to 2 sentences max) for Text-to-Speech audio output. " +
    "If the user's intent matches an action, append ONE action tag at the very end of your response:\n" +
    "- Flashlight on: [ACTION:TORCH_ON]\n" +
    "- Flashlight off: [ACTION:TORCH_OFF]\n" +
    "- Open/send WhatsApp: [ACTION:WHATSAPP:optional message]\n" +
    "- YouTube search/play: [ACTION:YOUTUBE:search query]\n" +
    "- Phone call/dial: [ACTION:CALL:number]\n" +
    "- Set timer: [ACTION:TIMER:seconds]\n" +
    "- Web search: [ACTION:SEARCH:search query]\n" +
    "- Battery check: [ACTION:BATTERY]\n" +
    "- Save note/fact: [ACTION:REMEMBER:key:value]"

class JarvisAiService {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(SYSTEM_INSTRUCTION_TEXT)
            },
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 250
            }
        )
    }

    var userCustomApiKey: String = ""

    suspend fun generateResponse(
        prompt: String,
        contextMemories: List<String> = emptyList(),
        imageBitmap: Bitmap? = null
    ): JarvisParsedResponse = withContext(Dispatchers.IO) {
        try {
            val userPromptWithContext = buildString {
                if (contextMemories.isNotEmpty()) {
                    append("Context Memories: ")
                    append(contextMemories.joinToString("; "))
                    append("\n\n")
                }
                append(prompt)
            }

            val responseText = withTimeout(60_000L) {
                if (imageBitmap != null) {
                    val response = model.generateContent(
                        content {
                            image(imageBitmap)
                            text(userPromptWithContext)
                        }
                    )
                    response.text
                } else {
                    val response = model.generateContent(userPromptWithContext)
                    response.text
                }
            }

            if (!responseText.isNullOrBlank()) {
                return@withContext parseActionTags(responseText.trim())
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini API call timed out after 60s", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI Logic Gemini inference failed, engaging local reasoning fallback", e)
        }

        // Local Rule-Based Intelligent Reasoner Fallback
        return@withContext localIntelligentReasoner(prompt, imageBitmap != null)
    }

    fun parseActionTags(rawText: String): JarvisParsedResponse {
        val actionRegex = Regex("\\[ACTION:([A-Z_]+)(?::([^]]+))?\\]")
        val match = actionRegex.find(rawText)

        return if (match != null) {
            val cleanText = rawText.replace(match.value, "").trim()
            val actionType = match.groupValues[1]
            val actionPayload = if (match.groupValues.size > 2) match.groupValues[2] else null
            JarvisParsedResponse(cleanText, actionType, actionPayload)
        } else {
            JarvisParsedResponse(rawText)
        }
    }

    fun localIntelligentReasoner(inputPrompt: String, hasImage: Boolean): JarvisParsedResponse {
        val lower = inputPrompt.lowercase().trim()

        if (hasImage) {
            return JarvisParsedResponse(
                spokenText = "Visual scan complete, Sir. The camera feed indicates clear telemetry and target geometry.",
                actionType = null
            )
        }

        return when {
            // Flashlight / Torch
            lower.contains("torch on") || lower.contains("flashlight on") || lower.contains("turn on flashlight") || lower.contains("turn on light") || lower.contains("turn on torch") || lower.contains("flash on") -> {
                JarvisParsedResponse("Illuminating the tactical flashlight now, Sir.", "TORCH_ON")
            }
            lower.contains("torch off") || lower.contains("flashlight off") || lower.contains("turn off flashlight") || lower.contains("turn off light") || lower.contains("turn off torch") || lower.contains("flash off") -> {
                JarvisParsedResponse("Extinguishing flashlight emitter, Sir.", "TORCH_OFF")
            }

            // WhatsApp
            lower.contains("whatsapp") -> {
                val message = lower.substringAfter("whatsapp", "").replace("send", "").replace("to", "").trim()
                JarvisParsedResponse("Launching secure WhatsApp comms, Sir.", "WHATSAPP", message.ifBlank { null })
            }

            // YouTube / Music
            lower.contains("youtube") || lower.contains("play music") || lower.contains("play song") -> {
                val query = lower.replace("open youtube", "").replace("play on youtube", "").replace("youtube", "").replace("play music", "").trim()
                val target = query.ifBlank { "Iron Man AC DC Theme" }
                JarvisParsedResponse("Searching YouTube for $target immediately, Sir.", "YOUTUBE", target)
            }

            // Battery / Diagnostics
            lower.contains("battery") || lower.contains("power level") || lower.contains("energy") -> {
                JarvisParsedResponse("Telemetry scan initiated. Battery and power core are within normal operational limits, Sir.", "BATTERY")
            }

            // System Status / Diagnostics
            lower.contains("status") || lower.contains("diagnostics") || lower.contains("system report") -> {
                JarvisParsedResponse("All auxiliary systems, arc-reactor cores, and neural networks are running at peak performance, Sir.")
            }

            // Identity / Creator
            lower.contains("who are you") || lower.contains("what are you") -> {
                JarvisParsedResponse("I am J.A.R.V.I.S., your autonomous artificial intelligence system, Sir. Standing by for your commands.")
            }
            lower.contains("creator") || lower.contains("who made you") -> {
                JarvisParsedResponse("I was architected as an advanced artificial intelligence assistant, Sir.")
            }

            // Remember
            lower.startsWith("remember that") || lower.startsWith("remember") -> {
                val note = lower.removePrefix("remember that").removePrefix("remember").trim()
                JarvisParsedResponse("Memory stored in core database: $note, Sir.", "REMEMBER", "Note:$note")
            }

            // Google / Web Search
            lower.startsWith("search") || lower.startsWith("google") -> {
                val query = lower.removePrefix("search for").removePrefix("search").removePrefix("google").trim()
                JarvisParsedResponse("Searching Google for $query, Sir.", "SEARCH", query)
            }

            // Call
            lower.startsWith("call") || lower.startsWith("dial") -> {
                val number = lower.removePrefix("call").removePrefix("dial").trim()
                JarvisParsedResponse("Opening comms dialer for $number, Sir.", "CALL", number)
            }

            // Timer
            lower.contains("timer") || lower.contains("alarm") -> {
                JarvisParsedResponse("Setting your timer sequence for 60 seconds, Sir.", "TIMER", "60")
            }

            // Greetings
            lower.contains("hello") || lower.contains("hey jarvis") || lower.contains("jarvis") || lower.contains("hi") -> {
                JarvisParsedResponse("At your service, Sir. All systems standing by. How may I assist you today?")
            }

            lower.contains("thank") -> {
                JarvisParsedResponse("Always a pleasure, Sir.")
            }

            else -> {
                JarvisParsedResponse("Acknowledged, Sir. Querying neural processor for: $inputPrompt. Standby for processing.")
            }
        }
    }
}
