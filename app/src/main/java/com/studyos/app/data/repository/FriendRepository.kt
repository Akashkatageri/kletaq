package com.studyos.app.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.studyos.app.data.model.LeaderboardEntry
import com.studyos.app.data.model.UserProfile
import com.studyos.app.data.model.UserStats
import com.studyos.app.data.model.toUserStatsSafe
import kotlinx.coroutines.channels.awaitClose
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
    private fun resolveIdentity(profile: UserProfile?, docDisplayName: String? = null): String? {
        val username = profile?.username?.trim()
        if (!username.isNullOrBlank()) return username

        val docName = docDisplayName?.trim()
        if (!docName.isNullOrBlank()) return docName

        return null
    }

    suspend fun searchUsers(query: String, currentUserId: String): Result<List<LeaderboardEntry>> {
        if (query.isBlank()) return Result.success(emptyList())
        val cleanQuery = query.trim().lowercase()

        return try {
            val usersSnapshot = firestore.collection("users")
                .limit(40)
                .get()
                .await()

            val results = mutableListOf<LeaderboardEntry>()

            for (doc in usersSnapshot.documents) {
                val uid = doc.id
                if (uid == currentUserId) continue

                val profile = doc.toObject(UserProfile::class.java)
                val identity = resolveIdentity(profile, doc.getString("displayName")) ?: continue
                val email = profile?.email ?: doc.getString("email") ?: ""

                // Match substring in identity or email
                if (identity.lowercase().contains(cleanQuery) || email.lowercase().contains(cleanQuery)) {
                    val statsRef = firestore.collection("users")
                        .document(uid)
                        .collection("stats")
                        .document("user_stats")

                    val statsSnapshot = statsRef.get().await()
                    val stats = if (statsSnapshot.exists()) statsSnapshot.toUserStatsSafe() ?: UserStats() else UserStats()

                    results.add(
                        LeaderboardEntry(
                            uid = uid,
                            username = identity,
                            photoUrl = profile?.photoUrl ?: "",
                            currentLevel = stats.currentLevel,
                            totalXp = stats.totalXp,
                            weeklyXp = stats.weeklyXp,
                            monthlyXp = stats.monthlyXp,
                            streak = stats.studyStreak
                        )
                    )
                }
            }

            Result.success(results)
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

            // Fetch live profiles and stats for friends asynchronously using coroutine await
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val results = mutableListOf<LeaderboardEntry>()
                for (fUid in friendUids) {
                    try {
                        val userDoc = firestore.collection("users").document(fUid).get().await()
                        val statsDoc = firestore.collection("users").document(fUid).collection("stats").document("user_stats").get().await()

                        val profile = userDoc.toObject(UserProfile::class.java)
                        val identity = resolveIdentity(profile, userDoc.getString("displayName")) ?: continue
                        val stats = if (statsDoc.exists()) statsDoc.toUserStatsSafe() ?: UserStats() else UserStats()

                        results.add(
                            LeaderboardEntry(
                                uid = fUid,
                                username = identity,
                                photoUrl = profile?.photoUrl ?: "",
                                currentLevel = stats.currentLevel,
                                totalXp = stats.totalXp,
                                weeklyXp = stats.weeklyXp,
                                monthlyXp = stats.monthlyXp,
                                streak = stats.studyStreak
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FriendRepository", "Error fetching friend $fUid profile", e)
                    }
                }
                trySend(results)
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

            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                val results = mutableListOf<LeaderboardEntry>()
                for (senderUid in requestUids) {
                    try {
                        val userDoc = firestore.collection("users").document(senderUid).get().await()
                        val statsDoc = firestore.collection("users").document(senderUid).collection("stats").document("user_stats").get().await()

                        val profile = userDoc.toObject(UserProfile::class.java)
                        val identity = resolveIdentity(profile, userDoc.getString("displayName")) ?: continue
                        val stats = if (statsDoc.exists()) statsDoc.toUserStatsSafe() ?: UserStats() else UserStats()

                        results.add(
                            LeaderboardEntry(
                                uid = senderUid,
                                username = identity,
                                photoUrl = profile?.photoUrl ?: "",
                                currentLevel = stats.currentLevel,
                                totalXp = stats.totalXp,
                                weeklyXp = stats.weeklyXp,
                                monthlyXp = stats.monthlyXp,
                                streak = stats.studyStreak
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FriendRepository", "Error fetching friend request from $senderUid", e)
                    }
                }
                trySend(results)
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun getGlobalLeaderboard(
        category: String = "streak",
        limit: Int = 50
    ): Result<List<LeaderboardEntry>> {
        return try {
            // Fetch all users from Firestore
            val usersSnapshot = firestore.collection("users").limit(100).get().await()
            val entries = mutableListOf<LeaderboardEntry>()

            for (doc in usersSnapshot.documents) {
                val uid = doc.id
                val profile = doc.toObject(UserProfile::class.java)
                val identity = resolveIdentity(profile, doc.getString("displayName")) ?: continue

                val statsRef = firestore.collection("users")
                    .document(uid)
                    .collection("stats")
                    .document("user_stats")

                val statsSnapshot = statsRef.get().await()
                val stats = if (statsSnapshot.exists()) statsSnapshot.toUserStatsSafe() ?: UserStats() else UserStats()

                entries.add(
                    LeaderboardEntry(
                        uid = uid,
                        username = identity,
                        photoUrl = profile?.photoUrl ?: "",
                        currentLevel = stats.currentLevel,
                        totalXp = stats.totalXp,
                        weeklyXp = stats.weeklyXp,
                        monthlyXp = stats.monthlyXp,
                        streak = stats.studyStreak
                    )
                )
            }

            // Always sort by streak
            val sorted = entries.sortedByDescending { it.streak }

            // Assign ranks (1, 2, 3...)
            val ranked = sorted.take(limit).mapIndexed { index, item ->
                item.copy(rank = index + 1)
            }

            // Seed leaderboards collection asynchronously
            val batch = firestore.batch()
            for (entry in ranked) {
                val leaderDoc = firestore.collection("leaderboards")
                    .document("global")
                    .collection("entries")
                    .document(entry.uid)
                batch.set(leaderDoc, entry, SetOptions.merge())
            }
            batch.commit()

            Result.success(ranked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
