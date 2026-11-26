package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BangladeshData
import com.example.data.model.BloodRequest
import com.example.data.model.CommunityPost
import com.example.data.model.DmConversation
import com.example.data.model.DmMessage
import com.example.data.model.DonorCertificate
import com.example.data.model.DonorEligibilityUtils
import com.example.data.model.DonorSummary
import com.example.data.model.GroupChatMessage
import com.example.data.model.NoticeItem
import com.example.data.model.SuccessStory
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

class DonorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Google Sign-In required.")

    private fun <T> Flow<T>.wrapFirestoreCatch(
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

    fun observeUserProfile(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .wrapFirestoreCatch(uid, OperationType.GET, "users/$uid")

    fun observeHasAdminRecord(uid: String): Flow<String?> =
        db.collection("admins").document(uid)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.getString("role") else null
            }
            .wrapFirestoreCatch(uid, OperationType.GET, "admins/$uid")

    fun observeApprovedDonors(uid: String): Flow<List<DonorSummary>> =
        db.collection("donorDirectory")
            .whereEqualTo("approvalStatus", "approved")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DonorSummary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.totalDonations }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "donorDirectory")

    fun observeActiveBloodRequests(uid: String): Flow<List<BloodRequest>> =
        db.collection("bloodRequests")
            .whereEqualTo("status", "active")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BloodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "bloodRequests")

    fun observeMyBloodRequests(uid: String): Flow<List<BloodRequest>> =
        db.collection("bloodRequests")
            .whereEqualTo("requesterUid", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BloodRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "bloodRequests")

    fun observeConversations(uid: String): Flow<List<DmConversation>> =
        db.collection("privateMessages")
            .whereArrayContains("participantIds", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DmConversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastMessageTimestamp ?: it.updatedAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "privateMessages")

    fun observeMessages(uid: String, conversationId: String): Flow<List<DmMessage>> =
        db.collection("privateMessages").document(conversationId)
            .collection("messages")
            .whereArrayContains("participantIds", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DmMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "privateMessages/$conversationId/messages")

    fun observeCommunityChat(uid: String): Flow<List<GroupChatMessage>> =
        db.collection("chatMessages")
            .whereEqualTo("channel", "community")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(GroupChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "chatMessages")

    fun observePublicPosts(uid: String): Flow<List<CommunityPost>> =
        db.collection("userPosts")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "userPosts")

    fun observeMyPosts(uid: String): Flow<List<CommunityPost>> =
        db.collection("userPosts")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "userPosts")

    fun observeSuccessStories(uid: String): Flow<List<SuccessStory>> =
        db.collection("successStories")
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SuccessStory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "successStories")

    fun observeActiveNotices(uid: String): Flow<List<NoticeItem>> =
        db.collection("notices")
            .whereEqualTo("active", true)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(NoticeItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "notices")

    fun observeMyCertificates(uid: String): Flow<List<DonorCertificate>> =
        db.collection("certificates")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DonorCertificate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .wrapFirestoreCatch(uid, OperationType.LIST, "certificates")

    suspend fun saveInitialProfile(
        email: String,
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
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val userPath = "users/${uid ?: "unauthenticated"}"
        return try {
            val validUid = requireUserId()
            val cleanName = fullName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Full name is required." }
            val cleanBlood = bloodGroup.trim()
            require(cleanBlood in BangladeshData.BLOOD_GROUPS) { "Valid blood group is required." }
            val cleanDistrict = district.trim().take(60)
            require(cleanDistrict.isNotEmpty()) { "District is required." }
            val normalizedGender = if (gender.lowercase(Locale.US) == "female") "female" else "male"
            val isDonor = userType == "donor"
            val initialApproval = if (isDonor) "pending" else "approved"
            val coord = BangladeshData.coordForDistrict(cleanDistrict)
            val generatedDonorId = DonorEligibilityUtils.generateDonorId(validUid)

            val userPayload = mapOf<String, Any?>(
                "uid" to validUid,
                "email" to email.trim().take(160),
                "fullName" to cleanName,
                "phoneNumber" to phoneNumber.trim().take(30).ifEmpty { null },
                "nidNumber" to nidNumber.trim().take(40).ifEmpty { null },
                "birthday" to birthday.trim().take(20).ifEmpty { null },
                "weightKg" to weightKg.coerceIn(0L, 300L),
                "userType" to if (isDonor) "donor" else "seeker",
                "donorEnabled" to isDonor,
                "seekerEnabled" to !isDonor,
                "bloodGroup" to cleanBlood,
                "district" to cleanDistrict,
                "upazila" to upazila.trim().take(60).ifEmpty { null },
                "address" to address.trim().take(250).ifEmpty { null },
                "gender" to normalizedGender,
                "bio" to bio.trim().take(300).ifEmpty { null },
                "lat" to coord?.lat,
                "lng" to coord?.lng,
                "approvalStatus" to initialApproval,
                "role" to "user",
                "donorId" to generatedDonorId,
                "certificates" to emptyList<String>(),
                "totalDonations" to 0L,
                "totalUnitsDonated" to 0L,
                "lastDonationApprovalStatus" to "none",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            db.collection("users").document(validUid).set(userPayload).await()

            if (isDonor) {
                val directoryPayload = mapOf<String, Any?>(
                    "uid" to validUid,
                    "fullName" to cleanName,
                    "bloodGroup" to cleanBlood,
                    "district" to cleanDistrict,
                    "upazila" to upazila.trim().take(60).ifEmpty { null },
                    "gender" to normalizedGender,
                    "donorId" to generatedDonorId,
                    "approvalStatus" to "pending",
                    "role" to "user",
                    "lat" to coord?.lat,
                    "lng" to coord?.lng,
                    "totalDonations" to 0L,
                    "available" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                db.collection("donorDirectory").document(validUid).set(directoryPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
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
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val userPath = "users/${uid ?: "unauthenticated"}"
        return try {
            val validUid = requireUserId()
            check(validUid == existing.uid) { "Authenticated user mismatch." }
            val cleanName = fullName.trim().take(100)
            require(cleanName.isNotEmpty()) { "Full name is required." }
            require(bloodGroup in BangladeshData.BLOOD_GROUPS) { "Valid blood group is required." }
            val cleanDistrict = district.trim().take(60)
            require(cleanDistrict.isNotEmpty()) { "District is required." }
            val normalizedGender = if (gender.lowercase(Locale.US) == "female") "female" else "male"
            val coord = BangladeshData.coordForDistrict(cleanDistrict)
            val preserveLiveCoord = existing.district.equals(cleanDistrict, ignoreCase = true) &&
                existing.lat != null && existing.lng != null
            val targetLat = if (preserveLiveCoord) existing.lat else coord?.lat
            val targetLng = if (preserveLiveCoord) existing.lng else coord?.lng
            val newApprovalStatus = when {
                !donorEnabled -> "approved"
                existing.donorEnabled -> existing.approvalStatus
                else -> "pending"
            }

            val updates = mutableMapOf<String, Any>(
                "fullName" to cleanName,
                "weightKg" to weightKg.coerceIn(0L, 300L),
                "userType" to if (donorEnabled) "donor" else "seeker",
                "donorEnabled" to donorEnabled,
                "seekerEnabled" to !donorEnabled,
                "bloodGroup" to bloodGroup,
                "district" to cleanDistrict,
                "gender" to normalizedGender,
                "approvalStatus" to newApprovalStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (phoneNumber.isNotBlank()) updates["phoneNumber"] = phoneNumber.trim().take(30)
            if (nidNumber.isNotBlank()) updates["nidNumber"] = nidNumber.trim().take(40)
            if (birthday.isNotBlank()) updates["birthday"] = birthday.trim().take(20)
            if (upazila.isNotBlank()) updates["upazila"] = upazila.trim().take(60)
            if (address.isNotBlank()) updates["address"] = address.trim().take(250)
            if (bio.isNotBlank()) updates["bio"] = bio.trim().take(300)
            if (!profilePic.isNullOrBlank()) updates["profilePic"] = profilePic.trim().take(1000)
            if (targetLat != null && targetLng != null) {
                updates["lat"] = targetLat
                updates["lng"] = targetLng
            }

            db.collection("users").document(validUid).update(updates).await()

            if (donorEnabled) {
                val dirRef = db.collection("donorDirectory").document(validUid)
                val dirSnap = dirRef.get().await()
                if (dirSnap.exists()) {
                    val dirUpdates = mutableMapOf<String, Any>(
                        "fullName" to cleanName,
                        "bloodGroup" to bloodGroup,
                        "district" to cleanDistrict,
                        "gender" to normalizedGender,
                        "available" to available,
                        "approvalStatus" to newApprovalStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (upazila.isNotBlank()) dirUpdates["upazila"] = upazila.trim().take(60)
                    if (!profilePic.isNullOrBlank()) dirUpdates["profilePic"] = profilePic.trim().take(1000)
                    if (targetLat != null && targetLng != null) {
                        dirUpdates["lat"] = targetLat
                        dirUpdates["lng"] = targetLng
                    }
                    dirRef.update(dirUpdates).await()
                } else {
                    val dirCreate = mapOf<String, Any?>(
                        "uid" to validUid,
                        "fullName" to cleanName,
                        "bloodGroup" to bloodGroup,
                        "district" to cleanDistrict,
                        "upazila" to upazila.trim().take(60).ifEmpty { null },
                        "gender" to normalizedGender,
                        "profilePic" to profilePic?.trim()?.take(1000)?.ifEmpty { null },
                        "donorId" to (existing.donorId ?: DonorEligibilityUtils.generateDonorId(validUid)),
                        "approvalStatus" to "pending",
                        "role" to "user",
                        "lat" to targetLat,
                        "lng" to targetLng,
                        "lastDonationDate" to existing.lastDonationDate,
                        "totalDonations" to 0L,
                        "available" to available,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    dirRef.set(dirCreate).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userPath)
            Result.failure(e)
        }
    }

    suspend fun updateLiveLocation(
        existing: UserProfile,
        lat: Double,
        lng: Double
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        val userPath = "users/${uid ?: "unauthenticated"}"
        return try {
            val validUid = requireUserId()
            check(validUid == existing.uid) { "Authenticated user mismatch." }
            require(lat in -90.0..90.0 && lng in -180.0..180.0) { "Invalid GPS coordinates." }

            val updates = mapOf<String, Any>(
                "lat" to lat,
                "lng" to lng,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(validUid).update(updates).await()

            if (existing.donorEnabled) {
                val dirRef = db.collection("donorDirectory").document(validUid)
                val dirSnap = dirRef.get().await()
                if (dirSnap.exists()) {
                    dirRef.update(updates).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userPath)
            Result.failure(e)
        }
    }

    suspend fun submitDonationProof(
        uid: String,
        donationDate: String,
        units: Long,
        proofDetails: String,
        verificationCode: String
    ): Result<Unit> {
        val path = "users/$uid"
        return try {
            val validUid = requireUserId()
            check(validUid == uid) { "Authenticated user mismatch." }
            require(donationDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                "Enter donation date as YYYY-MM-DD."
            }
            val cleanUnits = units.coerceIn(1L, 10L)
            val updates = mutableMapOf<String, Any>(
                "lastDonationDate" to donationDate,
                "lastDonationUnits" to cleanUnits,
                "lastDonationApprovalStatus" to "pending",
                "lastDonationSubmittedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (proofDetails.isNotBlank()) updates["lastDonationProof"] = proofDetails.trim().take(1000)
            if (verificationCode.isNotBlank()) updates["lastDonationCode"] = verificationCode.trim().take(64)

            db.collection("users").document(validUid).update(updates).await()

            val dirRef = db.collection("donorDirectory").document(validUid)
            if (dirRef.get().await().exists()) {
                dirRef.update(
                    mapOf(
                        "lastDonationDate" to donationDate,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createBloodRequest(
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
    ): Result<Unit> {
        var path = "bloodRequests"
        return try {
            val uid = requireUserId()
            require(patientName.isNotBlank()) { "Patient name is required." }
            require(bloodGroup in BangladeshData.BLOOD_GROUPS) { "Valid blood group is required." }
            require(hospitalName.isNotBlank()) { "Hospital name is required." }
            require(contactNumber.trim().length >= 6) { "Valid contact number is required." }
            require(requiredBy.trim().length >= 4) { "Required date is required." }

            val docRef = db.collection("bloodRequests").document()
            path = docRef.path
            val payload = mapOf<String, Any?>(
                "requesterUid" to uid,
                "requesterName" to requesterName.trim().ifEmpty { "Member" }.take(100),
                "patientName" to patientName.trim().take(100),
                "patientAge" to patientAge?.coerceIn(0L, 130L),
                "patientGender" to patientGender?.trim()?.take(20)?.ifEmpty { null },
                "bloodGroup" to bloodGroup,
                "unitsNeeded" to unitsNeeded.coerceIn(1L, 20L),
                "hospitalName" to hospitalName.trim().take(150),
                "hospitalAddress" to hospitalAddress.trim().take(250).ifEmpty { null },
                "district" to district.trim().take(60),
                "contactNumber" to contactNumber.trim().take(30),
                "requiredBy" to requiredBy.trim().take(20),
                "requiredTime" to requiredTime.trim().take(20).ifEmpty { null },
                "urgency" to if (urgency in setOf("emergency", "urgent", "normal")) urgency else "urgent",
                "status" to "active",
                "notes" to notes.trim().take(500).ifEmpty { null },
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateBloodRequestStatus(requestId: String, newStatus: String): Result<Unit> {
        val path = "bloodRequests/$requestId"
        return try {
            requireUserId()
            require(newStatus in setOf("fulfilled", "cancelled")) { "Invalid request status." }
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

    fun conversationIdFor(uidA: String, uidB: String): String {
        val sorted = listOf(uidA, uidB).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    suspend fun openOrCreateConversation(
        myUid: String,
        myName: String,
        myBloodGroup: String?,
        peerUid: String,
        peerName: String,
        peerBloodGroup: String?
    ): Result<String> {
        val convId = conversationIdFor(myUid, peerUid)
        val path = "privateMessages/$convId"
        return try {
            val validUid = requireUserId()
            check(validUid == myUid)
            val convRef = db.collection("privateMessages").document(convId)
            val snap = convRef.get().await()
            if (!snap.exists()) {
                val participants = listOf(myUid, peerUid).sorted()
                val names = mapOf(
                    myUid to myName.trim().ifEmpty { "Member" },
                    peerUid to peerName.trim().ifEmpty { "Donor" }
                )
                val bloodGroups = buildMap {
                    if (!myBloodGroup.isNullOrBlank()) put(myUid, myBloodGroup)
                    if (!peerBloodGroup.isNullOrBlank()) put(peerUid, peerBloodGroup)
                }
                val payload = mapOf<String, Any?>(
                    "participantIds" to participants,
                    "participantNames" to names,
                    "participantBloodGroups" to bloodGroups.ifEmpty { null },
                    "lastMessage" to "",
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "unreadCounts" to mapOf(myUid to 0L, peerUid to 0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                convRef.set(payload).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sendDirectMessage(
        conversationId: String,
        senderUid: String,
        senderName: String,
        senderBloodGroup: String?,
        receiverUid: String,
        receiverName: String,
        receiverBloodGroup: String?,
        text: String
    ): Result<Unit> {
        val cleanText = text.trim().take(2000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val path = "privateMessages/$conversationId"
        return try {
            val validUid = requireUserId()
            check(validUid == senderUid)
            openOrCreateConversation(
                senderUid, senderName, senderBloodGroup,
                receiverUid, receiverName, receiverBloodGroup
            ).getOrThrow()

            val participants = listOf(senderUid, receiverUid).sorted()
            val convRef = db.collection("privateMessages").document(conversationId)
            val msgRef = convRef.collection("messages").document()

            val msgPayload = mapOf(
                "senderId" to senderUid,
                "receiverId" to receiverUid,
                "participantIds" to participants,
                "message" to cleanText,
                "readStatus" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            msgRef.set(msgPayload).await()

            val convSnap = convRef.get().await()
            val currentUnreadMap = (convSnap.get("unreadCounts") as? Map<*, *>) ?: emptyMap<Any?, Any?>()
            val receiverUnread = ((currentUnreadMap[receiverUid] as? Number)?.toLong() ?: 0L) + 1L

            convRef.update(
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                    "unreadCounts" to mapOf(senderUid to 0L, receiverUid to receiverUnread),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "$path/messages")
            Result.failure(e)
        }
    }

    suspend fun markConversationRead(conversationId: String, myUid: String, peerUid: String): Result<Unit> {
        val path = "privateMessages/$conversationId"
        return try {
            requireUserId()
            val convRef = db.collection("privateMessages").document(conversationId)
            val snap = convRef.get().await()
            if (snap.exists()) {
                val currentUnread = (snap.get("unreadCounts") as? Map<*, *>) ?: emptyMap<Any?, Any?>()
                val peerUnread = (currentUnread[peerUid] as? Number)?.toLong() ?: 0L
                if (((currentUnread[myUid] as? Number)?.toLong() ?: 0L) > 0L) {
                    convRef.update(
                        mapOf(
                            "unreadCounts" to mapOf(myUid to 0L, peerUid to peerUnread),
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

    suspend fun sendCommunityChatMessage(profile: UserProfile, role: String, message: String): Result<Unit> {
        val cleanMsg = message.trim().take(1000)
        if (cleanMsg.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        var path = "chatMessages"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("chatMessages").document()
            path = docRef.path
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "userName" to profile.fullName.ifEmpty { "Member" }.take(100),
                "userRole" to role.take(30),
                "donorEnabled" to profile.donorEnabled,
                "bloodGroup" to profile.bloodGroup?.take(8),
                "message" to cleanMsg,
                "channel" to "community",
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createCommunityPost(
        profile: UserProfile,
        text: String,
        imageUrl: String?,
        pollQuestion: String?,
        pollOptions: List<String>
    ): Result<Unit> {
        var path = "userPosts"
        return try {
            val uid = requireUserId()
            val cleanText = text.trim().take(2000)
            val cleanImage = imageUrl?.trim()?.take(1000)?.ifEmpty { null }
            val cleanQuestion = pollQuestion?.trim()?.take(200)?.ifEmpty { null }
            val validOptions = pollOptions.map { it.trim().take(100) }.filter { it.isNotEmpty() }.take(4)

            require(cleanText.isNotEmpty() || cleanImage != null || (cleanQuestion != null && validOptions.size >= 2)) {
                "Enter post text, photo, or a poll with at least 2 options."
            }

            val pollMap: Map<String, Any>? = if (cleanQuestion != null && validOptions.size >= 2) {
                mapOf(
                    "question" to cleanQuestion,
                    "options" to validOptions.mapIndexed { idx, opt ->
                        mapOf("id" to idx.toString(), "text" to opt, "voters" to emptyList<String>())
                    }
                )
            } else null

            val docRef = db.collection("userPosts").document()
            path = docRef.path
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "userName" to profile.fullName.ifEmpty { "Member" }.take(100),
                "userAvatar" to profile.profilePic?.take(1000),
                "gender" to profile.gender,
                "donorEnabled" to profile.donorEnabled,
                "bloodGroup" to profile.bloodGroup,
                "district" to profile.district,
                "text" to cleanText.ifEmpty { null },
                "imageUrl" to cleanImage,
                "visibility" to "public",
                "likesCount" to 0L,
                "likedBy" to emptyList<String>(),
                "comments" to emptyList<Map<String, Any>>(),
                "poll" to pollMap,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun togglePostLike(postId: String): Result<Unit> {
        val path = "userPosts/$postId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("userPosts").document(postId)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val existingLiked = (snap.get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val updatedLiked = if (uid in existingLiked) {
                    existingLiked - uid
                } else {
                    (existingLiked + uid).take(500)
                }
                tx.update(
                    docRef,
                    mapOf(
                        "likedBy" to updatedLiked,
                        "likesCount" to updatedLiked.size.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addPostComment(postId: String, userName: String, commentText: String): Result<Unit> {
        val cleanComment = commentText.trim().take(500)
        if (cleanComment.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val path = "userPosts/$postId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("userPosts").document(postId)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val existingComments = (snap.get("comments") as? List<*>)
                    ?.mapNotNull { it as? Map<*, *> }
                    ?.map { map -> map.entries.associate { (k, v) -> k.toString() to (v ?: "") } }
                    ?: emptyList()
                val newComment = mapOf<String, Any>(
                    "id" to UUID.randomUUID().toString().take(8),
                    "userId" to uid,
                    "userName" to userName.ifEmpty { "Member" }.take(100),
                    "text" to cleanComment,
                    "createdAtMillis" to System.currentTimeMillis()
                )
                val updated = (existingComments + newComment).takeLast(100)
                tx.update(
                    docRef,
                    mapOf(
                        "comments" to updated,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun voteOnPoll(postId: String, optionId: String): Result<Unit> {
        val path = "userPosts/$postId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("userPosts").document(postId)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val rawPoll = snap.get("poll") as? Map<*, *> ?: return@runTransaction
                val question = rawPoll["question"] as? String ?: ""
                val rawOptions = rawPoll["options"] as? List<*> ?: return@runTransaction
                val updatedOptions = rawOptions.mapIndexedNotNull { idx, entry ->
                    val map = entry as? Map<*, *> ?: return@mapIndexedNotNull null
                    val id = (map["id"] as? String) ?: idx.toString()
                    val text = map["text"] as? String ?: return@mapIndexedNotNull null
                    val oldVoters = (map["voters"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val cleanVoters = oldVoters.filter { it != uid }
                    val newVoters = if (id == optionId) cleanVoters + uid else cleanVoters
                    mapOf("id" to id, "text" to text, "voters" to newVoters)
                }
                tx.update(
                    docRef,
                    mapOf(
                        "poll" to mapOf("question" to question, "options" to updatedOptions),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCommunityPost(postId: String, text: String, imageUrl: String?): Result<Unit> {
        val path = "userPosts/$postId"
        return try {
            requireUserId()
            val cleanText = text.trim().take(2000)
            require(cleanText.isNotEmpty() || !imageUrl.isNullOrBlank()) {
                "Post text or photo cannot be empty."
            }
            val updates = mutableMapOf<String, Any>(
                "text" to cleanText,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!imageUrl.isNullOrBlank()) {
                updates["imageUrl"] = imageUrl.trim().take(1000)
            }
            db.collection("userPosts").document(postId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val path = "userPosts/$postId"
        return try {
            requireUserId()
            db.collection("userPosts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun reportPost(postId: String, reason: String): Result<Unit> {
        val cleanReason = reason.trim().take(500)
        if (cleanReason.length < 3) return Result.failure(IllegalArgumentException("Please describe the issue (min 3 chars)."))
        var path = "reports"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("reports").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "reporterUid" to uid,
                    "postId" to postId,
                    "reason" to cleanReason,
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp()
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

    suspend fun createSuccessStory(
        userName: String,
        title: String,
        story: String,
        hospitalName: String,
        bloodGroup: String
    ): Result<Unit> {
        var path = "successStories"
        return try {
            val uid = requireUserId()
            require(title.trim().length >= 3) { "Title must be at least 3 characters." }
            require(story.trim().length >= 10) { "Story must be at least 10 characters." }
            val docRef = db.collection("successStories").document()
            path = docRef.path
            val payload = mapOf<String, Any?>(
                "userId" to uid,
                "userName" to userName.ifEmpty { "Donor" }.take(100),
                "title" to title.trim().take(150),
                "story" to story.trim().take(4000),
                "hospitalName" to hospitalName.trim().take(150).ifEmpty { null },
                "bloodGroup" to bloodGroup.take(8).ifEmpty { null },
                "status" to "published",
                "likesCount" to 0L,
                "likedBy" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleStoryLike(storyId: String): Result<Unit> {
        val path = "successStories/$storyId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("successStories").document(storyId)
            db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val existing = (snap.get("likedBy") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val updated = if (uid in existing) existing - uid else (existing + uid).take(500)
                tx.update(
                    docRef,
                    mapOf(
                        "likedBy" to updated,
                        "likesCount" to updated.size.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun verifyCertificateById(certificateId: String): Result<DonorCertificate?> {
        val cleanId = certificateId.trim()
        val path = "certificates/$cleanId"
        return try {
            requireUserId()
            require(cleanId.matches(Regex("^[a-zA-Z0-9_\\-]+$")) && cleanId.length in 4..64) {
                "Enter a valid certificate ID (letters, numbers, hyphen or underscore)."
            }
            val snap = db.collection("certificates").document(cleanId).get().await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                Result.success(snap.toObject(DonorCertificate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun verifyDonorByDonorId(donorIdCode: String): Result<DonorSummary?> {
        val cleanCode = donorIdCode.trim().uppercase(Locale.US)
        return try {
            requireUserId()
            require(cleanCode.isNotEmpty()) { "Enter a Donor ID to verify." }
            val querySnap = db.collection("donorDirectory")
                .whereEqualTo("approvalStatus", "approved")
                .whereEqualTo("donorId", cleanCode)
                .get()
                .await()
            val first = querySnap.toObjects(DonorSummary::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .firstOrNull()
            Result.success(first)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "donorDirectory")
            Result.failure(e)
        }
    }
}
