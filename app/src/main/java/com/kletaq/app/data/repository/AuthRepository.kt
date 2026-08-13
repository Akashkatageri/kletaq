package com.kletaq.app.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import com.kletaq.app.widgets.data.WidgetDataHelper
import dagger.hilt.android.qualifiers.ApplicationContext

interface AuthRepository {
    val currentUser: FirebaseUser?
    suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser>
    suspend fun signInWithGoogleToken(idToken: String): Result<FirebaseUser>
    suspend fun signInAnonymously(): Result<FirebaseUser>
    suspend fun signInWithFallbackAccount(email: String?, displayName: String?): Result<FirebaseUser>
    fun signOut()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    override suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Google Sign-In returned null user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogleToken(idToken: String): Result<FirebaseUser> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithCredential(credential)
    }

    override suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Anonymous Sign-In returned null user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithFallbackAccount(email: String?, displayName: String?): Result<FirebaseUser> {
        val cleanEmail = if (!email.isNullOrBlank() && email.contains("@")) {
            email
        } else {
            "student_${System.currentTimeMillis()}@kletaq.app"
        }
        val password = "KletaqPass2026!"

        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user
            if (user != null) Result.success(user) else Result.failure(Exception("Sign in returned null"))
        } catch (e: Exception) {
            try {
                val createResult = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
                val user = createResult.user
                if (user != null) Result.success(user) else Result.failure(Exception("Creation returned null"))
            } catch (ex: Exception) {
                if (firebaseAuth.currentUser != null) {
                    Result.success(firebaseAuth.currentUser!!)
                } else {
                    Result.failure(ex)
                }
            }
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
        WidgetDataHelper.clearAndRefresh(context)
    }
}
