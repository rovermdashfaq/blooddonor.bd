package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DonorCertificate
import com.example.data.model.DonorEligibilityUtils
import com.example.data.model.DonorSummary
import com.example.data.model.NoticeItem
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.DonorRepository
import com.example.util.GeoPoint
import com.example.util.unwrapFirestoreMessage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VerificationLookupState {
    data object Idle : VerificationLookupState
    data object Searching : VerificationLookupState
    data class CertificateFound(val certificate: DonorCertificate) : VerificationLookupState
    data class DonorFound(val donor: DonorSummary) : VerificationLookupState
    data class NotFound(val query: String) : VerificationLookupState
    data class Error(val message: String) : VerificationLookupState
}

class MainSessionViewModel(
    private val repository: DonorRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val isEmailVerified: Boolean
) : ViewModel() {

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val adminRoleRecord: StateFlow<String?> = repository.observeHasAdminRecord(currentUserId)
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    val approvedDonors: StateFlow<UiState<List<DonorSummary>>> = repository.observeApprovedDonors(currentUserId)
        .map<List<DonorSummary>, UiState<List<DonorSummary>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing donorDirectory", root)
            emit(UiState.Error(root.message ?: "Failed to load donors"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val activeNotices: StateFlow<UiState<List<NoticeItem>>> = repository.observeActiveNotices(currentUserId)
        .map<List<NoticeItem>, UiState<List<NoticeItem>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            emit(UiState.Error(root.message ?: "Failed to load notices"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val myCertificates: StateFlow<UiState<List<DonorCertificate>>> = repository.observeMyCertificates(currentUserId)
        .map<List<DonorCertificate>, UiState<List<DonorCertificate>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            emit(UiState.Error(root.message ?: "Failed to load certificates"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _verificationState = MutableStateFlow<VerificationLookupState>(VerificationLookupState.Idle)
    val verificationState: StateFlow<VerificationLookupState> = _verificationState.asStateFlow()

    private val _myLiveLocation = MutableStateFlow<GeoPoint?>(null)
    val myLiveLocation: StateFlow<GeoPoint?> = _myLiveLocation.asStateFlow()

    private val _isLiveLocationSharing = MutableStateFlow(false)
    val isLiveLocationSharing: StateFlow<Boolean> = _isLiveLocationSharing.asStateFlow()

    fun isEffectiveAdmin(profile: UserProfile?, adminRecordRole: String?): Boolean {
        if (DonorEligibilityUtils.isSuperAdminEmail(currentUserEmail, isEmailVerified)) return true
        if (!adminRecordRole.isNullOrBlank()) return true
        return DonorEligibilityUtils.isAdminRole(profile?.role ?: "user")
    }

    fun effectiveRole(profile: UserProfile?, adminRecordRole: String?): String {
        if (DonorEligibilityUtils.isSuperAdminEmail(currentUserEmail, isEmailVerified)) return "main_admin"
        if (!adminRecordRole.isNullOrBlank()) return adminRecordRole
        return profile?.role ?: "user"
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun resetVerification() {
        _verificationState.value = VerificationLookupState.Idle
    }

    fun saveOnboarding(
        fullName: String,
        phoneNumber: String,
        nidNumber: String,
        birthday: String,
        weightKg: Long,
        userType: String,
        bloodGroup: String,
        district: String,
        upazila: String,
        address: String,
        gender: String,
        bio: String
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveInitialProfile(
                email = currentUserEmail,
                fullName = fullName,
                phoneNumber = phoneNumber,
                nidNumber = nidNumber,
                birthday = birthday,
                weightKg = weightKg,
                userType = userType,
                bloodGroup = bloodGroup,
                district = district,
                upazila = upazila,
                address = address,
                gender = gender,
                bio = bio
            )
            _isSaving.value = false
            res.onSuccess {
                _actionMessage.value = if (userType == "donor") {
                    "Profile created! Your donor registration is pending admin verification."
                } else {
                    "Seeker profile created successfully."
                }
            }.onFailure { err ->
                _actionMessage.value = unwrapFirestoreMessage(err, "Failed to save profile.")
            }
        }
    }

    fun updateProfile(
        existing: UserProfile,
        fullName: String,
        phoneNumber: String,
        nidNumber: String,
        birthday: String,
        weightKg: Long,
        donorEnabled: Boolean,
        bloodGroup: String,
        district: String,
        upazila: String,
        address: String,
        gender: String,
        bio: String,
        profilePic: String?,
        available: Boolean
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updateProfile(
                existing = existing,
                fullName = fullName,
                phoneNumber = phoneNumber,
                nidNumber = nidNumber,
                birthday = birthday,
                weightKg = weightKg,
                donorEnabled = donorEnabled,
                bloodGroup = bloodGroup,
                district = district,
                upazila = upazila,
                address = address,
                gender = gender,
                bio = bio,
                profilePic = profilePic,
                available = available
            )
            _isSaving.value = false
            res.onSuccess {
                _actionMessage.value = "Profile updated."
            }.onFailure { err ->
                _actionMessage.value = unwrapFirestoreMessage(err, "Failed to update profile.")
            }
        }
    }

    fun updateLiveGpsLocation(
        existing: UserProfile,
        point: GeoPoint,
        syncToDirectory: Boolean = true,
        showToast: Boolean = true
    ) {
        _myLiveLocation.value = point
        if (!syncToDirectory) return
        viewModelScope.launch {
            if (showToast) _isSaving.value = true
            val res = repository.updateLiveLocation(existing, point.lat, point.lng)
            if (showToast) _isSaving.value = false
            res.onSuccess {
                if (showToast) _actionMessage.value = "Live GPS location updated on map."
            }.onFailure { err ->
                if (showToast) _actionMessage.value = unwrapFirestoreMessage(err, "Failed to update live location.")
            }
        }
    }

    fun setLiveLocationSharing(enabled: Boolean) {
        _isLiveLocationSharing.value = enabled
        _actionMessage.value = if (enabled) {
            "Live GPS location sharing enabled."
        } else {
            "Live GPS location sharing paused."
        }
    }

    fun setLocalGpsPreview(point: GeoPoint) {
        _myLiveLocation.value = point
    }

    fun submitDonationProof(date: String, units: Long, proof: String, code: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.submitDonationProof(currentUserId, date, units, proof, code)
            _isSaving.value = false
            res.onSuccess {
                _actionMessage.value = "Donation proof submitted for admin verification."
            }.onFailure { err ->
                _actionMessage.value = unwrapFirestoreMessage(err, "Failed to submit donation proof.")
            }
        }
    }

    fun verifyCertificateOrDonor(query: String, verifyCertificate: Boolean) {
        val clean = query.trim()
        if (clean.isEmpty()) {
            _verificationState.value = VerificationLookupState.Error("Enter an ID to verify.")
            return
        }
        viewModelScope.launch {
            _verificationState.value = VerificationLookupState.Searching
            if (verifyCertificate) {
                repository.verifyCertificateById(clean)
                    .onSuccess { cert ->
                        _verificationState.value = if (cert != null) {
                            VerificationLookupState.CertificateFound(cert)
                        } else {
                            VerificationLookupState.NotFound(clean)
                        }
                    }
                    .onFailure { err ->
                        _verificationState.value = VerificationLookupState.Error(
                            unwrapFirestoreMessage(err, "Certificate verification failed.")
                        )
                    }
            } else {
                repository.verifyDonorByDonorId(clean)
                    .onSuccess { donor ->
                        _verificationState.value = if (donor != null) {
                            VerificationLookupState.DonorFound(donor)
                        } else {
                            VerificationLookupState.NotFound(clean)
                        }
                    }
                    .onFailure { err ->
                        _verificationState.value = VerificationLookupState.Error(
                            unwrapFirestoreMessage(err, "Donor ID lookup failed.")
                        )
                    }
            }
        }
    }

    private companion object {
        const val TAG = "MainSessionVM"
        const val STOP_TIMEOUT = 5000L
    }
}
