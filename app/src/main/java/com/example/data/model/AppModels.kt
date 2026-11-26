package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val role: String = "moderator",
    val grantedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String? = null,
    val nidNumber: String? = null,
    val birthday: String? = null,
    val weightKg: Long? = null,
    val userType: String = "donor",
    val donorEnabled: Boolean = true,
    val seekerEnabled: Boolean = false,
    val bloodGroup: String? = null,
    val district: String? = null,
    val upazila: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val profilePic: String? = null,
    val bio: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val approvalStatus: String = "pending",
    val role: String = "user",
    val donorId: String? = null,
    val certificates: List<String> = emptyList(),
    val totalDonations: Long = 0L,
    val totalUnitsDonated: Long = 0L,
    val lastDonationDate: String? = null,
    val lastDonationUnits: Long? = null,
    val lastDonationApprovalStatus: String = "none",
    val lastDonationProof: String? = null,
    val lastDonationCode: String? = null,
    val lastDonationSubmittedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DonorSummary(
    @DocumentId val id: String = "",
    val uid: String = "",
    val fullName: String = "",
    val bloodGroup: String = "",
    val district: String = "",
    val upazila: String? = null,
    val gender: String = "male",
    val profilePic: String? = null,
    val donorId: String? = null,
    val approvalStatus: String = "pending",
    val role: String = "user",
    val lat: Double? = null,
    val lng: Double? = null,
    val lastDonationDate: String? = null,
    val totalDonations: Long = 0L,
    val available: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BloodRequest(
    @DocumentId val id: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val patientName: String = "",
    val patientAge: Long? = null,
    val patientGender: String? = null,
    val bloodGroup: String = "",
    val unitsNeeded: Long = 1L,
    val hospitalName: String = "",
    val hospitalAddress: String? = null,
    val district: String = "",
    val contactNumber: String = "",
    val requiredBy: String = "",
    val requiredTime: String? = null,
    val urgency: String = "urgent",
    val status: String = "active",
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val fulfilledAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DmConversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantBloodGroups: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DmMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val participantIds: List<String> = emptyList(),
    val message: String = "",
    val readStatus: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userRole: String = "user",
    val donorEnabled: Boolean = false,
    val bloodGroup: String? = null,
    val message: String = "",
    val channel: String = "community",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val createdAtMillis: Long = 0L
)

@Keep
@IgnoreExtraProperties
data class PollOption(
    val id: String = "",
    val text: String = "",
    val voters: List<String> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class PollData(
    val question: String = "",
    val options: List<PollOption> = emptyList()
)

@Keep
@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String? = null,
    val gender: String? = null,
    val donorEnabled: Boolean = false,
    val bloodGroup: String? = null,
    val district: String? = null,
    val text: String? = null,
    val imageUrl: String? = null,
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val comments: List<Map<String, Any>> = emptyList(),
    val poll: Map<String, Any>? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedComments(): List<PostComment> = comments.mapNotNull { item ->
        val text = item["text"] as? String ?: return@mapNotNull null
        PostComment(
            id = item["id"] as? String ?: "",
            userId = item["userId"] as? String ?: "",
            userName = item["userName"] as? String ?: "Member",
            text = text,
            createdAtMillis = (item["createdAtMillis"] as? Number)?.toLong() ?: 0L
        )
    }

    fun parsedPoll(): PollData? {
        val rawPoll = poll ?: return null
        val question = rawPoll["question"] as? String ?: return null
        val rawOptions = rawPoll["options"] as? List<*> ?: return null
        val options = rawOptions.mapIndexedNotNull { index, entry ->
            val map = entry as? Map<*, *> ?: return@mapIndexedNotNull null
            val optText = map["text"] as? String ?: return@mapIndexedNotNull null
            val voters = (map["voters"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            PollOption(
                id = (map["id"] as? String) ?: index.toString(),
                text = optText,
                voters = voters
            )
        }
        return if (options.isNotEmpty()) PollData(question, options) else null
    }
}

@Keep
@IgnoreExtraProperties
data class SuccessStory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val title: String = "",
    val story: String = "",
    val hospitalName: String? = null,
    val bloodGroup: String? = null,
    val status: String = "published",
    val likesCount: Long = 0L,
    val likedBy: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DonorCertificate(
    @DocumentId val id: String = "",
    val certificateId: String = "",
    val userId: String = "",
    val userName: String = "",
    val donorId: String = "",
    val bloodGroup: String = "",
    val district: String? = null,
    val issuedBy: String = "",
    val status: String = "valid",
    val issuedDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NoticeItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val priority: String = "info",
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val postId: String = "",
    val reason: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AiChatMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    val isStreaming: Boolean = false
)

object DonorEligibilityUtils {
    private val isoFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun isSuperAdminEmail(email: String?, isEmailVerified: Boolean): Boolean {
        if (!isEmailVerified || email.isNullOrBlank()) return false
        return email.equals("mdashfaqurrahmantanim@gmail.com", ignoreCase = true) ||
            email.equals("blooddonorbd025@gmail.com", ignoreCase = true)
    }

    fun effectiveRole(profile: UserProfile?, email: String?, isEmailVerified: Boolean): String {
        if (isSuperAdminEmail(email, isEmailVerified)) return "main_admin"
        return profile?.role ?: "user"
    }

    fun isAdminRole(role: String): Boolean =
        role in setOf("main_admin", "admin", "moderator")

    fun daysSinceLastDonation(lastDonationDate: String?): Long? {
        if (lastDonationDate.isNullOrBlank()) return null
        return try {
            val parsed = isoFormat.parse(lastDonationDate) ?: return null
            val diffMs = System.currentTimeMillis() - parsed.time
            TimeUnit.MILLISECONDS.toDays(diffMs).coerceAtLeast(0L)
        } catch (_: Exception) {
            null
        }
    }

    fun daysUntilEligible(lastDonationDate: String?): Long {
        val daysSince = daysSinceLastDonation(lastDonationDate) ?: return 0L
        return (90L - daysSince).coerceAtLeast(0L)
    }

    fun isEligibleByAge(birthdayIso: String?): Boolean {
        if (birthdayIso.isNullOrBlank()) return true
        return try {
            val dob = isoFormat.parse(birthdayIso) ?: return false
            val dobCal = Calendar.getInstance().apply { time = dob }
            val nowCal = Calendar.getInstance()
            var age = nowCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            if (nowCal.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age >= 18
        } catch (_: Exception) {
            false
        }
    }

    fun calculateAge(birthdayIso: String?): Int? {
        if (birthdayIso.isNullOrBlank()) return null
        return try {
            val dob = isoFormat.parse(birthdayIso) ?: return null
            val dobCal = Calendar.getInstance().apply { time = dob }
            val nowCal = Calendar.getInstance()
            var age = nowCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            if (nowCal.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (_: Exception) {
            null
        }
    }

    fun isBirthdayToday(birthdayIso: String?): Boolean {
        if (birthdayIso.isNullOrBlank()) return false
        return try {
            val dob = isoFormat.parse(birthdayIso) ?: return false
            val dobCal = Calendar.getInstance().apply { time = dob }
            val nowCal = Calendar.getInstance()
            dobCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
                dobCal.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)
        } catch (_: Exception) {
            false
        }
    }

    fun formatTimestamp(ts: Timestamp?): String {
        val date = ts?.toDate() ?: Date()
        return SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(date)
    }

    fun generateDonorId(uid: String): String {
        val suffix = uid.filter { it.isLetterOrDigit() }.uppercase(Locale.US).take(6).padEnd(6, 'X')
        return "BD-$suffix"
    }
}
