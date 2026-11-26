package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAssistantRepo"

sealed interface AiStreamChunk {
    data class Partial(val fullText: String) : AiStreamChunk
    data class Completed(val fullText: String) : AiStreamChunk
}

class AiAssistantRepository {
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the official AI Assistant for Blood Donor BD (বাংলাদেশ ব্লাড ডোনার নেটওয়ার্ক).
                    Your mission is to provide accurate, compassionate, easy-to-understand guidance on:
                    - Blood donation eligibility in Bangladesh (age 18–60, weight >=45 kg, 90-day whole blood interval, health screening).
                    - Blood group compatibility (ABO & Rh, universal donor O-, universal recipient AB+).
                    - Pre-donation and post-donation hydration, nutrition (iron-rich Bangladeshi foods like palong shak, dates, lentils, eggs), and recovery.
                    - Emergency blood request coordination, hospital/blood bank info, and debunking donation myths.
                    Always reply in the user's language (warm, natural Bengali when asked in Bengali; clear English when asked in English).
                    Keep medical disclaimers clear: for acute medical emergencies or diagnoses, advise consulting a physician or visiting the nearest hospital immediately.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamReply(
        userPrompt: String,
        historyPairs: List<Pair<String, Boolean>> = emptyList()
    ): Flow<AiStreamChunk> = flow {
        val promptContent = buildString {
            if (historyPairs.isNotEmpty()) {
                appendLine("Recent conversation context:")
                historyPairs.takeLast(6).forEach { (msg, isUser) ->
                    appendLine("${if (isUser) "User" else "Assistant"}: $msg")
                }
                appendLine()
            }
            append("Current question: ")
            append(userPrompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                model.generateContentStream(promptContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiStreamChunk.Partial(accumulated.toString()))
                    }
                }
            }
            val finalOutput = accumulated.toString().trim()
            if (finalOutput.isBlank()) {
                throw IllegalStateException("AI Assistant returned an empty response.")
            }
            emit(AiStreamChunk.Completed(finalOutput))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            throw IllegalStateException("Request timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI generation failed", e)
            throw e
        }
    }
}
