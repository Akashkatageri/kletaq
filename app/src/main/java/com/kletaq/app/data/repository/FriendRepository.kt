package com.kletaq.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.kletaq.app.data.model.FriendshipStatus
import com.kletaq.app.data.model.LeaderboardEntry
import com.kletaq.app.data.model.UserPublicProfile
import com.kletaq.app.data.model.UserProfile
import com.kletaq.app.data.model.UserStats
import com.kletaq.app.data.model.toUserStatsSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun isValidStudentIdentity(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val trimmed = name.trim()
        val lower = trimmed.lowercase()
        if (lower.startsWith("test") || lower.endsWith("test") || lower.contains("testing")) return false
        if (lower.contains("unavailable") || lower == "student" || lower == "unknown") return false
        // Exclude raw Firebase Auth UIDs (20+ chars alphanumeric without spaces with mixed cases/digits)
        if (trimmed.length >= 20 && !trimmed.contains(" ") && trimmed.any { it.isDigit() } && trimmed.any { it.isLetter() }) {
            return false
        }
        return true
    }

    private fun resolveIdentity(profile: UserProfile?, docDisplayName: String? = null): String? {
        val username = profile?.username?.trim()
        if (!username.isNullOrBlank() && isValidStudentIdentity(username)) return username

        val docName = docDisplayName?.trim()
        if (!docName.isNullOrBlank() && isValidStudentIdentity(docName)) return docName

        return null
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            cleanupStaleTestLeaderboardEntries()
        }
    }

    suspend fun cleanupStaleTestLeaderboardEntries() {
        try {
            val entriesSnap = firestore.collection("leaderboards").document("global").collection("entries").get().await()
            val batch = firestore.batch()
            var hasDeletes = false
            for (doc in entriesSnap.documents) {
                val username = doc.getString("username") ?: ""
                val uid = doc.getString("uid") ?: doc.id
                if (!isValidStudentIdentity(username) || !isValidStudentIdentity(uid) || doc.id.lowercase().startsWith("test") || uid.lowercase().startsWith("test")) {
                    batch.delete(doc.reference)
                    hasDeletes = true
                }
            }
            if (hasDeletes) {
                batch.commit().await()
            }
        } catch (_: Exception) {}
    }

    suspend fun searchUsers(query: String, currentUserId: String): Result<List<LeaderboardEntry>> {
        if (query.isBlank()) return Result.success(emptyList())
        val cleanQuery = query.trim().lowercase()

        return try {
            val usersSnapshot = firestore.collection("users")
                .limit(40)
                .get()
                .await()

            coroutineScope {
                val deferred = usersSnapshot.documents.map { doc ->
                    async(Dispatchers.IO) {
                        val uid = doc.id
                        if (uid == currentUserId) return@async null

                        val profile = doc.toObject(UserProfile::class.java)
                        val identity = resolveIdentity(profile, doc.getString("displayName")) ?: return@async null
                        val email = profile?.email ?: doc.getString("email") ?: ""

                        if (identity.lowercase().contains(cleanQuery) || email.lowercase().contains(cleanQuery)) {
                            val statsSnapshot = try {
                                firestore.collection("stats").document(uid).get().await()
                            } catch (_: Exception) { null }

                            val stats = if (statsSnapshot != null && statsSnapshot.exists()) {
                                statsSnapshot.toUserStatsSafe() ?: UserStats()
                            } else {
                                UserStats()
                            }

                            LeaderboardEntry(
                                uid = uid,
                                username = identity,
                                photoUrl = profile?.photoUrl ?: "",
                                currentLevel = stats.currentLevel,
                                totalXp = stats.totalXp,
                                weeklyXp = stats.weeklyXp,
                                monthlyXp = stats.monthlyXp,
                                streak = stats.effectiveStreak
                            )
                        } else {
                            null
                        }
                    }
                }
                Result.success(deferred.awaitAll().filterNotNull())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(currentUserId: String, targetUserId: String): Result<Unit> {
        if (currentUserId.isBlank() || targetUserId.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid User ID"))
        }
        if (currentUserId == targetUserId) {
            return Result.failure(IllegalArgumentException("Cannot add yourself as a friend"))
        }
        return try {
            // Fetch sender profile to resolve real identity
            val senderDoc = firestore.collection("users").document(currentUserId).get().await()
            val profile = senderDoc.toObject(UserProfile::class.java)
            val senderName = resolveIdentity(profile, senderDoc.getString("displayName"))

            if (senderName.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Please set a username before sending friend requests."))
            }

            val batch = firestore.batch()

            val ref = firestore.collection("users")
                .document(targetUserId)
                .collection("friend_requests")
                .document(currentUserId)

            val data = mapOf(
                "uid" to currentUserId,
                "timestamp" to FieldValue.serverTimestamp()
            )
            batch.set(ref, data, SetOptions.merge())

            // Also create in-app notification for recipient
            val notifRef = firestore.collection("users")
                .document(targetUserId)
                .collection("notifications")
                .document()

            val notifData = mapOf(
                "id" to notifRef.id,
                "title" to "Friend request",
                "message" to "$senderName wants to connect with you.",
                "type" to "friend_request",
                "read" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "entityId" to currentUserId,
                "entityType" to "friend_request"
            )
            batch.set(notifRef, notifData)

            batch.commit().await()
            android.util.Log.d("FriendRepository", "Successfully sent friend request from $currentUserId ($senderName) to $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FriendRepository", "Failed to send friend request from $currentUserId to $targetUserId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(currentUserId: String, fromUserId: String): Result<Unit> {
        return try {
            val batch = firestore.batch()

            // Add to current user's friends
            val myFriendDoc = firestore.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(fromUserId)
            batch.set(myFriendDoc, mapOf("uid" to fromUserId, "addedAt" to FieldValue.serverTimestamp()))

            // Add to friend's friends
            val theirFriendDoc = firestore.collection("users")
                .document(fromUserId)
                .collection("friends")
                .document(currentUserId)
            batch.set(theirFriendDoc, mapOf("uid" to currentUserId, "addedAt" to FieldValue.serverTimestamp()))

            // Delete incoming request
            val requestDoc = firestore.collection("users")
                .document(currentUserId)
                .collection("friend_requests")
                .document(fromUserId)
            batch.delete(requestDoc)

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectFriendRequest(currentUserId: String, fromUserId: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(currentUserId)
                .collection("friend_requests")
                .document(fromUserId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFriendsFlow(currentUserId: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("users")
            .document(currentUserId)
            .collection("friends")

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val friendUids = snapshot.documents.map { it.id }
            if (friendUids.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            // Fetch live profiles and stats for friends concurrently in parallel
            CoroutineScope(Dispatchers.IO).launch {
                val deferred = friendUids.map { fUid ->
                    async(Dispatchers.IO) {
                        try {
                            val userDoc = firestore.collection("users").document(fUid).get().await()
                            val statsDoc = firestore.collection("stats").document(fUid).get().await()

                            val profile = userDoc.toObject(UserProfile::class.java)
                            val identity = resolveIdentity(profile, userDoc.getString("displayName")) ?: return@async null
                            val stats = if (statsDoc.exists()) statsDoc.toUserStatsSafe() ?: UserStats() else UserStats()

                            LeaderboardEntry(
                                uid = fUid,
                                username = identity,
                                photoUrl = profile?.photoUrl ?: "",
                                currentLevel = stats.currentLevel,
                                totalXp = stats.totalXp,
                                weeklyXp = stats.weeklyXp,
                                monthlyXp = stats.monthlyXp,
                                streak = stats.effectiveStreak
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("FriendRepository", "Error fetching friend $fUid profile", e)
                            null
                        }
                    }
                }
                trySend(deferred.awaitAll().filterNotNull())
            }
        }

        awaitClose { listener.remove() }
    }

    fun getFriendRequestsFlow(currentUserId: String): Flow<List<LeaderboardEntry>> = callbackFlow {
        if (currentUserId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firestore.collection("users")
            .document(currentUserId)
            .collection("friend_requests")

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val requestUids = snapshot.documents.map { it.id }
            if (requestUids.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            // Fetch live profiles and stats for incoming requests concurrently in parallel
            CoroutineScope(Dispatchers.IO).launch {
                val deferred = requestUids.map { senderUid ->
                    async(Dispatchers.IO) {
                        try {
                            val userDoc = firestore.collection("users").document(senderUid).get().await()
                            val statsDoc = firestore.collection("stats").document(senderUid).get().await()

                            val profile = userDoc.toObject(UserProfile::class.java)
                            val identity = resolveIdentity(profile, userDoc.getString("displayName")) ?: return@async null
                            val stats = if (statsDoc.exists()) statsDoc.toUserStatsSafe() ?: UserStats() else UserStats()

                            LeaderboardEntry(
                                uid = senderUid,
                                username = identity,
                                photoUrl = profile?.photoUrl ?: "",
                                currentLevel = stats.currentLevel,
                                totalXp = stats.totalXp,
                                weeklyXp = stats.weeklyXp,
                                monthlyXp = stats.monthlyXp,
                                streak = stats.effectiveStreak
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("FriendRepository", "Error fetching friend request from $senderUid", e)
                            null
                        }
                    }
                }
                trySend(deferred.awaitAll().filterNotNull())
            }
        }

        awaitClose { listener.remove() }
    }

    fun getGlobalLeaderboardFlow(
        category: String = "streak",
        limit: Int = 50
    ): Flow<List<LeaderboardEntry>> = callbackFlow {
        val sortField = if (category.equals("xp", ignoreCase = true)) "totalXp" else "streak"

        val query = firestore.collection("leaderboards")
            .document("global")
            .collection("entries")
            .orderBy(sortField, Query.Direction.DESCENDING)
            .limit(limit.toLong())

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FriendRepository", "Error listening to global leaderboard: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val entries = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(LeaderboardEntry::class.java)
                }.filter { isValidStudentIdentity(it.username) && !it.uid.lowercase().startsWith("test") }
                .mapIndexed { index, entry ->
                    entry.copy(rank = index + 1)
                }
                if (entries.isNotEmpty()) {
                    trySend(entries)
                    return@addSnapshotListener
                }
            }

            // If empty (e.g. first run before anyone has written), seed from registered users
            CoroutineScope(Dispatchers.IO).launch {
                val seeded = seedLeaderboardFromUsers(category, limit)
                trySend(seeded)
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun getGlobalLeaderboard(
        category: String = "streak",
        limit: Int = 50
    ): Result<List<LeaderboardEntry>> = coroutineScope {
        try {
            val sortField = if (category.equals("xp", ignoreCase = true)) "totalXp" else "streak"

            // 1-SHOT INDEXED QUERY (<100ms): Fetch from indexed collection directly in 1 network call
            val snapshot = firestore.collection("leaderboards")
                .document("global")
                .collection("entries")
                .orderBy(sortField, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val entries = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(LeaderboardEntry::class.java)
                }.filter { isValidStudentIdentity(it.username) && !it.uid.lowercase().startsWith("test") }
                .mapIndexed { index, entry ->
                    entry.copy(rank = index + 1)
                }
                if (entries.isNotEmpty()) {
                    return@coroutineScope Result.success(entries)
                }
            }

            // Fallback: seed database and return ranked list
            val seeded = seedLeaderboardFromUsers(category, limit)
            Result.success(seeded)
        } catch (e: Exception) {
            android.util.Log.e("FriendRepository", "Error in getGlobalLeaderboard, falling back to seed", e)
            try {
                val fallback = seedLeaderboardFromUsers(category, limit)
                Result.success(fallback)
            } catch (fallbackEx: Exception) {
                Result.failure(fallbackEx)
            }
        }
    }

    suspend fun syncUserLeaderboardEntry(uid: String, stats: UserStats) {
        if (uid.isBlank()) return
        try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            val profile = userDoc.toObject(UserProfile::class.java)
            val identity = resolveIdentity(profile, userDoc.getString("displayName")) ?: return
            if (!isValidStudentIdentity(identity) || uid.equals("test", ignoreCase = true)) return
            val photoUrl = profile?.photoUrl ?: userDoc.getString("photoUrl") ?: ""

            val entry = LeaderboardEntry(
                uid = uid,
                username = identity,
                photoUrl = photoUrl,
                currentLevel = stats.currentLevel,
                totalXp = stats.totalXp,
                weeklyXp = stats.weeklyXp,
                monthlyXp = stats.monthlyXp,
                streak = stats.effectiveStreak
            )

            firestore.collection("leaderboards")
                .document("global")
                .collection("entries")
                .document(uid)
                .set(entry, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            android.util.Log.e("FriendRepository", "Error syncing user leaderboard entry for $uid", e)
        }
    }

    private suspend fun seedLeaderboardFromUsers(category: String, limit: Int): List<LeaderboardEntry> = coroutineScope {
        try {
            val usersSnapshot = firestore.collection("users").limit(100).get().await()

            val deferredEntries = usersSnapshot.documents.map { doc ->
                async(Dispatchers.IO) {
                    val uid = doc.id
                    val profile = doc.toObject(UserProfile::class.java)
                    val identity = resolveIdentity(profile, doc.getString("displayName")) ?: return@async null

                    val statsRef = firestore.collection("stats").document(uid)
                    val statsSnapshot = try {
                        statsRef.get().await()
                    } catch (_: Exception) {
                        null
                    }
                    val stats = if (statsSnapshot != null && statsSnapshot.exists()) {
                        statsSnapshot.toUserStatsSafe() ?: UserStats()
                    } else {
                        UserStats()
                    }

                    LeaderboardEntry(
                        uid = uid,
                        username = identity,
                        photoUrl = profile?.photoUrl ?: "",
                        currentLevel = stats.currentLevel,
                        totalXp = stats.totalXp,
                        weeklyXp = stats.weeklyXp,
                        monthlyXp = stats.monthlyXp,
                        streak = stats.effectiveStreak
                    )
                }
            }

            val entries = deferredEntries.awaitAll().filterNotNull()

            // Sort by category (XP vs Streak)
            val sorted = if (category.equals("xp", ignoreCase = true)) {
                entries.sortedByDescending { it.totalXp }
            } else {
                entries.sortedByDescending { it.streak }
            }

            // Assign ranks (1, 2, 3...)
            val ranked = sorted.take(limit).mapIndexed { index, item ->
                item.copy(rank = index + 1)
            }

            // Seed global entries collection in Firestore asynchronously in batch
            launch(Dispatchers.IO) {
                try {
                    val batch = firestore.batch()
                    for (entry in entries) {
                        val leaderDoc = firestore.collection("leaderboards")
                            .document("global")
                            .collection("entries")
                            .document(entry.uid)
                        batch.set(leaderDoc, entry, SetOptions.merge())
                    }
                    batch.commit().await()
                } catch (_: Exception) {}
            }

            ranked
        } catch (e: Exception) {
            android.util.Log.e("FriendRepository", "Error seeding leaderboard from users", e)
            emptyList()
        }
    }

    suspend fun checkFriendshipStatus(currentUserId: String, targetUserId: String): FriendshipStatus {
        if (currentUserId.isBlank() || targetUserId.isBlank()) return FriendshipStatus.NOT_FRIENDS
        if (currentUserId == targetUserId) return FriendshipStatus.SELF

        return try {
            val isFriend = firestore.collection("users").document(currentUserId)
                .collection("friends").document(targetUserId).get().await().exists()
            if (isFriend) return FriendshipStatus.FRIENDS

            val isIncoming = firestore.collection("users").document(currentUserId)
                .collection("friend_requests").document(targetUserId).get().await().exists()
            if (isIncoming) return FriendshipStatus.REQUEST_RECEIVED

            val isOutgoing = firestore.collection("users").document(targetUserId)
                .collection("friend_requests").document(currentUserId).get().await().exists()
            if (isOutgoing) return FriendshipStatus.REQUEST_SENT

            FriendshipStatus.NOT_FRIENDS
        } catch (e: Exception) {
            android.util.Log.e("FriendRepository", "Error checking friendship status", e)
            FriendshipStatus.NOT_FRIENDS
        }
    }

    suspend fun getUserPublicProfile(
        currentUserId: String,
        targetUserId: String,
        fallbackRank: Int = 0
    ): Result<UserPublicProfile> {
        return try {
            val userDoc = firestore.collection("users").document(targetUserId).get().await()
            val statsDoc = firestore.collection("stats").document(targetUserId).get().await()

            val profile = userDoc.toObject(UserProfile::class.java)
            val stats = if (statsDoc.exists()) statsDoc.toUserStatsSafe() ?: UserStats() else UserStats()
            val identity = resolveIdentity(profile, userDoc.getString("displayName")) ?: "Classmate"
            val friendshipStatus = checkFriendshipStatus(currentUserId, targetUserId)

            Result.success(
                UserPublicProfile(
                    uid = targetUserId,
                    username = identity,
                    photoUrl = profile?.photoUrl ?: "",
                    branch = profile?.branch ?: "",
                    semester = profile?.semester ?: profile?.currentSemester ?: 0,
                    university = profile?.university ?: "VTU",
                    studyWhy = profile?.studyWhy ?: "",
                    stats = stats,
                    rank = fallbackRank,
                    friendshipStatus = friendshipStatus
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("FriendRepository", "Error getting user public profile for $targetUserId", e)
            Result.failure(e)
        }
    }
}
