package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AdminRecord
import com.example.data.model.BangladeshData
import com.example.data.model.BloodRequest
import com.example.data.model.DonorCertificate
import com.example.data.model.DonorEligibilityUtils
import com.example.data.model.NoticeItem
import com.example.data.model.PostReport
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BulkDonorInput(
    val fullName: String,
    val bloodGroup: String,
    val district: String,
    val gender: String = "male",
    val upazila: String = "",
    val totalDonations: Long = 0L
)

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAdminAuth(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Google Sign-In required.")

    private fun <T> Flow<T>.wrapAdminCatch(
        expectedUid: String,
        operation: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operation, path)
        }
        throw error
    }

    fun observeAllUsers(adminUid: String): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapAdminCatch(adminUid, OperationType.LIST, "users")

    fun observeAllRequests(adminUid: String): Flow<List<BloodRequest>> =
        db.collection("bloodRequests")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BloodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapAdminCatch(adminUid, OperationType.LIST, "bloodRequests")

    fun observeAllCertificates(adminUid: String): Flow<List<DonorCertificate>> =
        db.collection("certificates")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DonorCertificate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapAdminCatch(adminUid, OperationType.LIST, "certificates")

    fun observeAllNotices(adminUid: String): Flow<List<NoticeItem>> =
        db.collection("notices")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(NoticeItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapAdminCatch(adminUid, OperationType.LIST, "notices")

    fun observeAllReports(adminUid: String): Flow<List<PostReport>> =
        db.collection("reports")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PostReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapAdminCatch(adminUid, OperationType.LIST, "reports")

    fun observeAdminsList(adminUid: String): Flow<List<AdminRecord>> =
        db.collection("admins")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .wrapAdminCatch(adminUid, OperationType.LIST, "admins")

    suspend fun approveOrRejectDonor(user: UserProfile, approve: Boolean): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            requireAdminAuth()
            val newStatus = if (approve) "approved" else "rejected"
            val donorId = user.donorId?.takeIf { it.isNotBlank() }
                ?: DonorEligibilityUtils.generateDonorId(user.uid)

            db.collection("users").document(user.uid).update(
                mapOf(
                    "approvalStatus" to newStatus,
                    "donorId" to donorId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val dirRef = db.collection("donorDirectory").document(user.uid)
            val dirSnap = dirRef.get().await()
            if (dirSnap.exists()) {
                dirRef.update(
                    mapOf(
                        "approvalStatus" to newStatus,
                        "donorId" to donorId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (approve && user.donorEnabled && !user.bloodGroup.isNullOrBlank() && !user.district.isNullOrBlank()) {
                val createPayload = mapOf<String, Any?>(
                    "uid" to user.uid,
                    "fullName" to user.fullName,
                    "bloodGroup" to user.bloodGroup,
                    "district" to user.district,
                    "upazila" to user.upazila,
                    "gender" to (user.gender ?: "male"),
                    "profilePic" to user.profilePic,
                    "donorId" to donorId,
                    "approvalStatus" to "approved",
                    "role" to user.role,
                    "lat" to user.lat,
                    "lng" to user.lng,
                    "lastDonationDate" to user.lastDonationDate,
                    "totalDonations" to user.totalDonations,
                    "available" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                dirRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewDonationProof(user: UserProfile, approve: Boolean): Result<Unit> {
        val path = "users/${user.uid}"
        return try {
            requireAdminAuth()
            val newStatus = if (approve) "approved" else "rejected"
            val unitsToAdd = if (approve) (user.lastDonationUnits ?: 1L).coerceIn(1L, 10L) else 0L
            val updates = mutableMapOf<String, Any>(
                "lastDonationApprovalStatus" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (approve) {
                updates["totalDonations"] = FieldValue.increment(1L)
                updates["totalUnitsDonated"] = FieldValue.increment(unitsToAdd)
            }
            db.collection("users").document(user.uid).update(updates).await()

            if (approve) {
                val dirRef = db.collection("donorDirectory").document(user.uid)
                if (dirRef.get().await().exists()) {
                    dirRef.update(
                        mapOf(
                            "totalDonations" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun issueCertificate(targetUser: UserProfile, issuedByUid: String): Result<String> {
        var path = "certificates"
        return try {
            requireAdminAuth()
            val bg = targetUser.bloodGroup?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalArgumentException("User must have a blood group before certificate issuance."))
            val donorId = targetUser.donorId?.takeIf { it.isNotBlank() }
                ?: DonorEligibilityUtils.generateDonorId(targetUser.uid)
            val stamp = SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(Date())
            val certId = "CERT-${donorId.replace("-", "")}-$stamp"
            path = "certificates/$certId"
            val issuedDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())

            val payload = mapOf<String, Any?>(
                "certificateId" to certId,
                "userId" to targetUser.uid,
                "userName" to targetUser.fullName.ifEmpty { "Donor" }.take(100),
                "donorId" to donorId,
                "bloodGroup" to bg,
                "district" to targetUser.district,
                "issuedBy" to issuedByUid,
                "status" to "valid",
                "issuedDate" to issuedDate,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("certificates").document(certId).set(payload).await()
            db.collection("users").document(targetUser.uid).update(
                mapOf(
                    "certificates" to FieldValue.arrayUnion(certId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(certId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCertificateStatus(certId: String, newStatus: String): Result<Unit> {
        val path = "certificates/$certId"
        return try {
            requireAdminAuth()
            require(newStatus in setOf("valid", "revoked"))
            db.collection("certificates").document(certId).update("status", newStatus).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createNotice(title: String, message: String, priority: String, adminUid: String): Result<Unit> {
        var path = "notices"
        return try {
            requireAdminAuth()
            require(title.trim().length >= 2 && message.trim().length >= 2) {
                "Notice title and message are required."
            }
            val docRef = db.collection("notices").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "title" to title.trim().take(150),
                    "message" to message.trim().take(2000),
                    "priority" to if (priority in setOf("info", "normal", "urgent")) priority else "info",
                    "active" to true,
                    "createdBy" to adminUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleNoticeActive(noticeId: String, active: Boolean): Result<Unit> {
        val path = "notices/$noticeId"
        return try {
            requireAdminAuth()
            db.collection("notices").document(noticeId).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNotice(noticeId: String): Result<Unit> {
        val path = "notices/$noticeId"
        return try {
            requireAdminAuth()
            db.collection("notices").document(noticeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun resolveReport(reportId: String): Result<Unit> {
        val path = "reports/$reportId"
        return try {
            requireAdminAuth()
            db.collection("reports").document(reportId).update("status", "resolved").await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReportedPost(postId: String, reportId: String): Result<Unit> {
        return try {
            requireAdminAuth()
            db.collection("userPosts").document(postId).delete().await()
            db.collection("reports").document(reportId).update("status", "resolved").await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "userPosts/$postId")
            Result.failure(e)
        }
    }

    suspend fun setUserAdminRole(
        targetUser: UserProfile,
        newRole: String,
        grantedByUid: String
    ): Result<Unit> {
        val path = "admins/${targetUser.uid}"
        return try {
            requireAdminAuth()
            require(newRole in setOf("user", "moderator", "admin", "main_admin"))
            val adminRef = db.collection("admins").document(targetUser.uid)
            if (newRole == "user") {
                if (adminRef.get().await().exists()) {
                    adminRef.delete().await()
                }
            } else {
                adminRef.set(
                    mapOf(
                        "uid" to targetUser.uid,
                        "email" to targetUser.email,
                        "role" to newRole,
                        "grantedBy" to grantedByUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            db.collection("users").document(targetUser.uid).update(
                mapOf(
                    "role" to newRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val dirRef = db.collection("donorDirectory").document(targetUser.uid)
            if (dirRef.get().await().exists()) {
                dirRef.update(
                    mapOf(
                        "role" to newRole,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateRequestStatusAdmin(requestId: String, newStatus: String): Result<Unit> {
        val path = "bloodRequests/$requestId"
        return try {
            requireAdminAuth()
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newStatus == "fulfilled") {
                updates["fulfilledAt"] = FieldValue.serverTimestamp()
            }
            db.collection("bloodRequests").document(requestId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun bulkRegisterDonors(entries: List<BulkDonorInput>): Result<Int> {
        val path = "donorDirectory"
        return try {
            requireAdminAuth()
            val validEntries = entries.filter {
                it.fullName.isNotBlank() &&
                    it.bloodGroup.trim() in BangladeshData.BLOOD_GROUPS &&
                    it.district.isNotBlank()
            }
            require(validEntries.isNotEmpty()) {
                "No valid donor rows found. Format: Name, BloodGroup, District, Gender(optional), Upazila(optional), Donations(optional)."
            }

            validEntries.chunked(200).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { entry ->
                    val bulkUid = "bulk_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                    val donorId = DonorEligibilityUtils.generateDonorId(bulkUid)
                    val cleanDist = entry.district.trim().take(60)
                    val coord = BangladeshData.coordForDistrict(cleanDist)
                    val gender = if (entry.gender.trim().lowercase(Locale.US) == "female") "female" else "male"
                    val payload = mapOf<String, Any?>(
                        "uid" to bulkUid,
                        "fullName" to entry.fullName.trim().take(100),
                        "bloodGroup" to entry.bloodGroup.trim(),
                        "district" to cleanDist,
                        "upazila" to entry.upazila.trim().take(60).ifEmpty { null },
                        "gender" to gender,
                        "donorId" to donorId,
                        "approvalStatus" to "approved",
                        "role" to "user",
                        "lat" to coord?.lat,
                        "lng" to coord?.lng,
                        "totalDonations" to entry.totalDonations.coerceIn(0L, 1000L),
                        "available" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    batch.set(db.collection("donorDirectory").document(bulkUid), payload)
                }
                batch.commit().await()
            }
            Result.success(validEntries.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }
}
