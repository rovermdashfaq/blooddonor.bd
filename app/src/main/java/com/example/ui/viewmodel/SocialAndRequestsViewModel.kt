package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BloodRequest
import com.example.data.model.CommunityPost
import com.example.data.model.DmConversation
import com.example.data.model.DmMessage
import com.example.data.model.GroupChatMessage
import com.example.data.model.SuccessStory
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.DonorRepository
import com.example.util.unwrapFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SocialAndRequestsViewModel(
    private val repository: DonorRepository,
    val currentUserId: String
) : ViewModel() {

    val activeRequests: StateFlow<UiState<List<BloodRequest>>> = repository.observeActiveBloodRequests(currentUserId)
        .map<List<BloodRequest>, UiState<List<BloodRequest>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load blood requests"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val myRequests: StateFlow<UiState<List<BloodRequest>>> = repository.observeMyBloodRequests(currentUserId)
        .map<List<BloodRequest>, UiState<List<BloodRequest>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load your requests"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val conversations: StateFlow<UiState<List<DmConversation>>> = repository.observeConversations(currentUserId)
        .map<List<DmConversation>, UiState<List<DmConversation>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load conversations"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    val dmMessages: StateFlow<UiState<List<DmMessage>>> = _selectedConversationId
        .flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(currentUserId, convId)
                    .map<List<DmMessage>, UiState<List<DmMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val fsError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (fsError == null && error is CancellationException) throw error
                        emit(UiState.Error((fsError ?: error).message ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Success(emptyList()))

    val communityChat: StateFlow<UiState<List<GroupChatMessage>>> = repository.observeCommunityChat(currentUserId)
        .map<List<GroupChatMessage>, UiState<List<GroupChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load group chat"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val publicPosts: StateFlow<UiState<List<CommunityPost>>> = repository.observePublicPosts(currentUserId)
        .map<List<CommunityPost>, UiState<List<CommunityPost>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load community feed"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val successStories: StateFlow<UiState<List<SuccessStory>>> = repository.observeSuccessStories(currentUserId)
        .map<List<SuccessStory>, UiState<List<SuccessStory>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load success stories"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun selectConversation(conversationId: String?, peerUid: String? = null) {
        _selectedConversationId.value = conversationId
        if (!conversationId.isNullOrBlank() && !peerUid.isNullOrBlank()) {
            viewModelScope.launch {
                repository.markConversationRead(conversationId, currentUserId, peerUid)
            }
        }
    }

    fun startDmWithDonor(
        myProfile: UserProfile,
        peerUid: String,
        peerName: String,
        peerBloodGroup: String?,
        onConversationReady: (String) -> Unit
    ) {
        if (peerUid == currentUserId) {
            _statusBanner.value = "You cannot start a direct message with yourself."
            return
        }
        viewModelScope.launch {
            repository.openOrCreateConversation(
                myUid = currentUserId,
                myName = myProfile.fullName,
                myBloodGroup = myProfile.bloodGroup,
                peerUid = peerUid,
                peerName = peerName,
                peerBloodGroup = peerBloodGroup
            ).onSuccess { convId ->
                _selectedConversationId.value = convId
                onConversationReady(convId)
            }.onFailure { err ->
                _statusBanner.value = unwrapFirestoreMessage(err, "Unable to open conversation.")
            }
        }
    }

    fun sendDm(
        conversationId: String,
        myProfile: UserProfile,
        peerUid: String,
        peerName: String,
        peerBloodGroup: String?,
        messageText: String
    ) {
        viewModelScope.launch {
            repository.sendDirectMessage(
                conversationId = conversationId,
                senderUid = currentUserId,
                senderName = myProfile.fullName,
                senderBloodGroup = myProfile.bloodGroup,
                receiverUid = peerUid,
                receiverName = peerName,
                receiverBloodGroup = peerBloodGroup,
                text = messageText
            ).onFailure { err ->
                _statusBanner.value = unwrapFirestoreMessage(err, "Failed to send message.")
            }
        }
    }

    fun sendGroupChat(profile: UserProfile, role: String, message: String) {
        viewModelScope.launch {
            repository.sendCommunityChatMessage(profile, role, message)
                .onFailure { err ->
                    _statusBanner.value = unwrapFirestoreMessage(err, "Failed to send chat message.")
                }
        }
    }

    fun createBloodRequest(
        requesterName: String,
        patientName: String,
        patientAge: Long?,
        patientGender: String?,
        bloodGroup: String,
        unitsNeeded: Long,
        hospitalName: String,
        hospitalAddress: String,
        district: String,
        contactNumber: String,
        requiredBy: String,
        requiredTime: String,
        urgency: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.createBloodRequest(
                requesterName = requesterName,
                patientName = patientName,
                patientAge = patientAge,
                patientGender = patientGender,
                bloodGroup = bloodGroup,
                unitsNeeded = unitsNeeded,
                hospitalName = hospitalName,
                hospitalAddress = hospitalAddress,
                district = district,
                contactNumber = contactNumber,
                requiredBy = requiredBy,
                requiredTime = requiredTime,
                urgency = urgency,
                notes = notes
            ).onSuccess {
                _statusBanner.value = "Emergency blood request posted."
            }.onFailure { err ->
                _statusBanner.value = unwrapFirestoreMessage(err, "Failed to create blood request.")
            }
        }
    }

    fun markRequestStatus(requestId: String, status: String) {
        viewModelScope.launch {
            repository.updateBloodRequestStatus(requestId, status)
                .onSuccess {
                    _statusBanner.value = "Request marked as $status."
                }
                .onFailure { err ->
                    _statusBanner.value = unwrapFirestoreMessage(err, "Failed to update request.")
                }
        }
    }

    fun createPost(
        profile: UserProfile,
        text: String,
        imageUrl: String?,
        pollQuestion: String?,
        pollOptions: List<String>
    ) {
        viewModelScope.launch {
            repository.createCommunityPost(profile, text, imageUrl, pollQuestion, pollOptions)
                .onSuccess {
                    _statusBanner.value = "Post published to Community Feed."
                }
                .onFailure { err ->
                    _statusBanner.value = unwrapFirestoreMessage(err, "Failed to publish post.")
                }
        }
    }

    fun togglePostLike(postId: String) {
        viewModelScope.launch {
            repository.togglePostLike(postId).onFailure { err ->
                Log.w(TAG, "Like toggle failed", err)
            }
        }
    }

    fun addComment(postId: String, userName: String, text: String) {
        viewModelScope.launch {
            repository.addPostComment(postId, userName, text).onFailure { err ->
                _statusBanner.value = unwrapFirestoreMessage(err, "Failed to add comment.")
            }
        }
    }

    fun votePoll(postId: String, optionId: String) {
        viewModelScope.launch {
            repository.voteOnPoll(postId, optionId).onFailure { err ->
                _statusBanner.value = unwrapFirestoreMessage(err, "Failed to cast vote.")
            }
        }
    }

    fun editPost(postId: String, text: String, imageUrl: String?) {
        viewModelScope.launch {
            repository.updateCommunityPost(postId, text, imageUrl)
                .onSuccess { _statusBanner.value = "Post updated." }
                .onFailure { err -> _statusBanner.value = unwrapFirestoreMessage(err, "Failed to update post.") }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
                .onSuccess { _statusBanner.value = "Post deleted." }
                .onFailure { err -> _statusBanner.value = unwrapFirestoreMessage(err, "Failed to delete post.") }
        }
    }

    fun reportPost(postId: String, reason: String) {
        viewModelScope.launch {
            repository.reportPost(postId, reason)
                .onSuccess { _statusBanner.value = "Report submitted for moderator review." }
                .onFailure { err -> _statusBanner.value = unwrapFirestoreMessage(err, "Failed to submit report.") }
        }
    }

    fun createStory(userName: String, title: String, story: String, hospitalName: String, bloodGroup: String) {
        viewModelScope.launch {
            repository.createSuccessStory(userName, title, story, hospitalName, bloodGroup)
                .onSuccess { _statusBanner.value = "Success story published!" }
                .onFailure { err -> _statusBanner.value = unwrapFirestoreMessage(err, "Failed to publish story.") }
        }
    }

    fun toggleStoryLike(storyId: String) {
        viewModelScope.launch {
            repository.toggleStoryLike(storyId)
        }
    }

    private companion object {
        const val TAG = "SocialRequestsVM"
        const val STOP_TIMEOUT = 5000L
    }
}
