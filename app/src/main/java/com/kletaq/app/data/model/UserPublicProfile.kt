package com.kletaq.app.data.model

enum class FriendshipStatus {
    SELF,
    FRIENDS,
    REQUEST_SENT,
    REQUEST_RECEIVED,
    NOT_FRIENDS
}

data class UserPublicProfile(
    val uid: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val branch: String = "",
    val semester: Int = 0,
    val university: String = "VTU",
    val studyWhy: String = "",
    val stats: UserStats = UserStats(),
    val rank: Int = 0,
    val friendshipStatus: FriendshipStatus = FriendshipStatus.NOT_FRIENDS
)
