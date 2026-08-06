package com.studyos.app.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class XpTransaction(
    val id: String = "",
    val amount: Long = 0L,
    val source: String = "",
    val referenceId: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)
