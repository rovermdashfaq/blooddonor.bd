package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiChatMessage
import com.example.data.repository.AiAssistantRepository
import com.example.data.repository.AiStreamChunk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AiChatUiState {
    data object Idle : AiChatUiState
    data object Loading : AiChatUiState
    data class Error(val message: String) : AiChatUiState
}

class AiAssistantViewModel(
    private val repository: AiAssistantRepository = AiAssistantRepository()
) : ViewModel() {

    private val welcomeMessage = AiChatMessage(
        id = "welcome",
        text = "আসসালামু আলাইকুম! আমি Blood Donor BD-এর অফিসিয়াল এআই সহকারী। রক্তদান, রক্তের গ্রুপ, যোগ্যতা ও স্বাস্থ্যবিধি নিয়ে বাংলা বা ইংরেজিতে যেকোনো প্রশ্ন করতে পারেন।",
        fromUser = false
    )

    private val _messages = MutableStateFlow(listOf(welcomeMessage))
    val messages: StateFlow<List<AiChatMessage>> = _messages.asStateFlow()

    private val _chatState = MutableStateFlow<AiChatUiState>(AiChatUiState.Idle)
    val chatState: StateFlow<AiChatUiState> = _chatState.asStateFlow()

    private var streamJob: Job? = null

    fun sendMessage(userText: String) {
        val prompt = userText.trim()
        if (prompt.isEmpty() || _chatState.value == AiChatUiState.Loading) return

        val history = _messages.value
            .filter { it.id != "welcome" && !it.isStreaming }
            .map { it.text to it.fromUser }

        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = prompt,
            fromUser = true
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val streamingPlaceholder = AiChatMessage(
            id = assistantMsgId,
            text = "...",
            fromUser = false,
            isStreaming = true
        )

        _messages.value = _messages.value + userMsg + streamingPlaceholder
        _chatState.value = AiChatUiState.Loading

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            try {
                repository.streamReply(prompt, history).collect { chunk ->
                    val updatedText = when (chunk) {
                        is AiStreamChunk.Partial -> chunk.fullText
                        is AiStreamChunk.Completed -> chunk.fullText
                    }
                    val stillStreaming = chunk is AiStreamChunk.Partial
                    _messages.value = _messages.value.map { msg ->
                        if (msg.id == assistantMsgId) {
                            msg.copy(text = updatedText, isStreaming = stillStreaming)
                        } else msg
                    }
                }
                _chatState.value = AiChatUiState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Assistant stream failed", e)
                _messages.value = _messages.value.filterNot { it.id == assistantMsgId }
                _chatState.value = AiChatUiState.Error(
                    e.localizedMessage ?: "Failed to reach Blood Donor BD AI Assistant."
                )
            }
        }
    }

    fun resetConversation() {
        streamJob?.cancel()
        streamJob = null
        _messages.value = listOf(welcomeMessage)
        _chatState.value = AiChatUiState.Idle
    }

    fun clearError() {
        if (_chatState.value is AiChatUiState.Error) {
            _chatState.value = AiChatUiState.Idle
        }
    }

    private companion object {
        const val TAG = "AiAssistantVM"
    }
}
