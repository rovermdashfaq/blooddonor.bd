package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BloodRequest
import com.example.data.model.DonorCertificate
import com.example.data.model.NoticeItem
import com.example.data.model.PostReport
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRepository
import com.example.data.repository.BulkDonorInput
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

class AdminViewModel(
    private val adminRepository: AdminRepository,
    val adminUid: String
) : ViewModel() {

    val allUsers: StateFlow<UiState<List<UserProfile>>> = adminRepository.observeAllUsers(adminUid)
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load users"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val allRequests: StateFlow<UiState<List<BloodRequest>>> = adminRepository.observeAllRequests(adminUid)
        .map<List<BloodRequest>, UiState<List<BloodRequest>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load requests"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val allCertificates: StateFlow<UiState<List<DonorCertificate>>> = adminRepository.observeAllCertificates(adminUid)
        .map<List<DonorCertificate>, UiState<List<DonorCertificate>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load certificates"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val allNotices: StateFlow<UiState<List<NoticeItem>>> = adminRepository.observeAllNotices(adminUid)
        .map<List<NoticeItem>, UiState<List<NoticeItem>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load notices"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val allReports: StateFlow<UiState<List<PostReport>>> = adminRepository.observeAllReports(adminUid)
        .map<List<PostReport>, UiState<List<PostReport>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsError == null && error is CancellationException) throw error
            emit(UiState.Error((fsError ?: error).message ?: "Failed to load reports"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    private val _adminToast = MutableStateFlow<String?>(null)
    val adminToast: StateFlow<String?> = _adminToast.asStateFlow()

    fun clearToast() {
        _adminToast.value = null
    }

    fun reviewDonor(user: UserProfile, approve: Boolean) {
        viewModelScope.launch {
            adminRepository.approveOrRejectDonor(user, approve)
                .onSuccess {
                    _adminToast.value = "${user.fullName} ${if (approve) "approved" else "rejected"}."
                }
                .onFailure { err ->
                    _adminToast.value = unwrapFirestoreMessage(err, "Donor action failed.")
                }
        }
    }

    fun reviewDonationProof(user: UserProfile, approve: Boolean) {
        viewModelScope.launch {
            adminRepository.reviewDonationProof(user, approve)
                .onSuccess {
                    _adminToast.value = "Donation proof ${if (approve) "approved & count updated" else "rejected"}."
                }
                .onFailure { err ->
                    _adminToast.value = unwrapFirestoreMessage(err, "Proof review failed.")
                }
        }
    }

    fun issueCertificate(user: UserProfile) {
        viewModelScope.launch {
            adminRepository.issueCertificate(user, adminUid)
                .onSuccess { certId ->
                    _adminToast.value = "Issued certificate $certId for ${user.fullName}."
                }
                .onFailure { err ->
                    _adminToast.value = unwrapFirestoreMessage(err, "Certificate issuance failed.")
                }
        }
    }

    fun setCertificateStatus(certId: String, status: String) {
        viewModelScope.launch {
            adminRepository.updateCertificateStatus(certId, status)
                .onSuccess { _adminToast.value = "Certificate marked $status." }
                .onFailure { err -> _adminToast.value = unwrapFirestoreMessage(err, "Update failed.") }
        }
    }

    fun createNotice(title: String, message: String, priority: String) {
        viewModelScope.launch {
            adminRepository.createNotice(title, message, priority, adminUid)
                .onSuccess { _adminToast.value = "Notice published." }
                .onFailure { err -> _adminToast.value = unwrapFirestoreMessage(err, "Notice creation failed.") }
        }
    }

    fun toggleNotice(noticeId: String, active: Boolean) {
        viewModelScope.launch {
            adminRepository.toggleNoticeActive(noticeId, active)
        }
    }

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch {
            adminRepository.deleteNotice(noticeId)
                .onSuccess { _adminToast.value = "Notice deleted." }
        }
    }

    fun resolveReport(reportId: String) {
        viewModelScope.launch {
            adminRepository.resolveReport(reportId)
                .onSuccess { _adminToast.value = "Report resolved." }
        }
    }

    fun deleteReportedPost(postId: String, reportId: String) {
        viewModelScope.launch {
            adminRepository.deleteReportedPost(postId, reportId)
                .onSuccess { _adminToast.value = "Post removed and report resolved." }
        }
    }

    fun updateRole(user: UserProfile, newRole: String) {
        viewModelScope.launch {
            adminRepository.setUserAdminRole(user, newRole, adminUid)
                .onSuccess { _adminToast.value = "${user.fullName} role set to $newRole." }
                .onFailure { err -> _adminToast.value = unwrapFirestoreMessage(err, "Role update failed.") }
        }
    }

    fun updateRequestStatus(requestId: String, status: String) {
        viewModelScope.launch {
            adminRepository.updateRequestStatusAdmin(requestId, status)
                .onSuccess { _adminToast.value = "Request updated to $status." }
        }
    }

    fun bulkRegisterDonors(entries: List<BulkDonorInput>) {
        viewModelScope.launch {
            adminRepository.bulkRegisterDonors(entries)
                .onSuccess { count -> _adminToast.value = "Bulk imported $count verified donors!" }
                .onFailure { err -> _adminToast.value = unwrapFirestoreMessage(err, "Bulk import failed.") }
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5000L
    }
}
