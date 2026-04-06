package com.example.brunnenapp.data

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.brunnenapp.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.security.SecureRandom

private const val Tag = "BrunnenAuth"

class AuthClient(context: Context) {
    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUserId: String? get() = firebaseAuth.currentUser?.uid

    fun restoreSession(): AuthenticatedAccount? {
        val user = firebaseAuth.currentUser ?: return null
        return AuthenticatedAccount(
            userId = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: user.email ?: user.uid,
            avatarUrl = user.photoUrl?.toString(),
        )
    }

    suspend fun signInWithBottomSheet(activity: Activity): AuthenticatedAccount {
        val primaryRequest = buildAuthorizedAccountsRequest()
        return try {
            signInWithRequest(activity, primaryRequest)
        } catch (_: NoCredentialException) {
            signInWithRequest(activity, buildAllAccountsRequest())
        }
    }

    suspend fun signInWithGoogleButton(activity: Activity): AuthenticatedAccount {
        return signInWithRequest(activity, buildExplicitGoogleButtonRequest())
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            Log.w(Tag, "clearCredentialState failed", e)
        }
    }

    suspend fun hasNickname(): Boolean {
        val uid = currentUserId ?: return false
        val doc = firestore.collection("users").document(uid).get().await()
        val nickname = doc.getString("nickname")
        return !nickname.isNullOrBlank()
    }

    suspend fun isNicknameAvailable(nickname: String): Boolean {
        val result = firestore.collection("users")
            .whereEqualTo("nickname", nickname)
            .get()
            .await()
        return result.isEmpty
    }

    suspend fun saveNickname(nickname: String) {
        val uid = currentUserId ?: error("Not signed in")
        val user = firebaseAuth.currentUser ?: error("Not signed in")
        val data = mapOf(
            "uid" to uid,
            "email" to (user.email ?: ""),
            "nickname" to nickname,
            "avatarUrl" to (user.photoUrl?.toString() ?: ""),
            "points" to 0L,
            "checkInCount" to 0L,
            "friends" to emptyList<String>(),
        )
        firestore.collection("users").document(uid).set(data, SetOptions.merge()).await()
    }

    suspend fun getUserProfile(): UserProfile? {
        val uid = currentUserId ?: return null
        val doc = firestore.collection("users").document(uid).get().await()
        if (!doc.exists()) return null
        return UserProfile(
            uid = doc.getString("uid") ?: uid,
            email = doc.getString("email") ?: "",
            nickname = doc.getString("nickname") ?: "",
            avatarUrl = doc.getString("avatarUrl"),
            points = doc.getLong("points") ?: 0L,
            checkInCount = doc.getLong("checkInCount") ?: 0L,
            friends = (doc.get("friends") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
        )
    }

    suspend fun recordCheckIn(fountain: Fountain, points: Int) {
        val uid = currentUserId ?: error("Not signed in")
        val checkInData = mapOf(
            "userId" to uid,
            "fountainId" to fountain.id,
            "fountainName" to fountain.name,
            "timestamp" to System.currentTimeMillis(),
            "points" to points,
        )
        firestore.collection("checkins").add(checkInData).await()

        val userRef = firestore.collection("users").document(uid)
        firestore.runTransaction { tx ->
            val snapshot = tx.get(userRef)
            val currentPoints = snapshot.getLong("points") ?: 0L
            val currentCount = snapshot.getLong("checkInCount") ?: 0L
            tx.update(userRef, mapOf(
                "points" to currentPoints + points,
                "checkInCount" to currentCount + 1,
            ))
        }.await()
    }

    suspend fun getLeaderboard(): List<UserProfile> {
        val result = firestore.collection("users")
            .orderBy("points", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        return result.documents.mapNotNull { doc ->
            UserProfile(
                uid = doc.getString("uid") ?: doc.id,
                email = doc.getString("email") ?: "",
                nickname = doc.getString("nickname") ?: "",
                avatarUrl = doc.getString("avatarUrl"),
                points = doc.getLong("points") ?: 0L,
                checkInCount = doc.getLong("checkInCount") ?: 0L,
                friends = (doc.get("friends") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            )
        }
    }

    suspend fun getFriendsLeaderboard(): List<UserProfile> {
        val profile = getUserProfile() ?: return emptyList()
        if (profile.friends.isEmpty()) return emptyList()
        val friendUids = profile.friends.take(10)
        val result = firestore.collection("users")
            .whereIn("uid", friendUids)
            .get()
            .await()
        return result.documents.mapNotNull { doc ->
            UserProfile(
                uid = doc.getString("uid") ?: doc.id,
                email = doc.getString("email") ?: "",
                nickname = doc.getString("nickname") ?: "",
                avatarUrl = doc.getString("avatarUrl"),
                points = doc.getLong("points") ?: 0L,
                checkInCount = doc.getLong("checkInCount") ?: 0L,
            )
        }.sortedByDescending { it.points }
    }

    suspend fun addFriendByNickname(nickname: String): Boolean {
        val uid = currentUserId ?: return false
        val result = firestore.collection("users")
            .whereEqualTo("nickname", nickname)
            .get()
            .await()
        val friendDoc = result.documents.firstOrNull() ?: return false
        val friendUid = friendDoc.getString("uid") ?: return false
        if (friendUid == uid) return false

        val userRef = firestore.collection("users").document(uid)
        firestore.runTransaction { tx ->
            val snapshot = tx.get(userRef)
            val friends = (snapshot.get("friends") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList()
                ?: mutableListOf()
            if (friendUid !in friends) {
                friends.add(friendUid)
                tx.update(userRef, "friends", friends)
            }
        }.await()
        return true
    }

    private suspend fun signInWithRequest(
        activity: Activity,
        requestSpec: CredentialRequestSpec,
    ): AuthenticatedAccount {
        val response = credentialManager.getCredential(
            context = activity,
            request = requestSpec.request,
        )
        val googleCredential = response.toGoogleCredential(requestSpec.rawNonce)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        firebaseAuth.signInWithCredential(firebaseCredential).await()
        val firebaseUser = firebaseAuth.currentUser
            ?: error("Firebase sign-in failed.")

        return AuthenticatedAccount(
            userId = firebaseUser.uid,
            email = firebaseUser.email ?: googleCredential.email,
            displayName = firebaseUser.displayName ?: googleCredential.displayName ?: googleCredential.email,
            avatarUrl = firebaseUser.photoUrl?.toString() ?: googleCredential.profilePictureUri,
        )
    }

    private fun buildAuthorizedAccountsRequest(): CredentialRequestSpec {
        val rawNonce = generateSecureRandomNonce()
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setAutoSelectEnabled(true)
            .setNonce(rawNonce.sha256Hex())
            .build()
        return CredentialRequestSpec(request = buildRequest(option), rawNonce = rawNonce)
    }

    private fun buildAllAccountsRequest(): CredentialRequestSpec {
        val rawNonce = generateSecureRandomNonce()
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setNonce(rawNonce.sha256Hex())
            .build()
        return CredentialRequestSpec(request = buildRequest(option), rawNonce = rawNonce)
    }

    private fun buildExplicitGoogleButtonRequest(): CredentialRequestSpec {
        val rawNonce = generateSecureRandomNonce()
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.WEB_CLIENT_ID)
            .setNonce(rawNonce.sha256Hex())
            .build()
        return CredentialRequestSpec(request = buildRequest(option), rawNonce = rawNonce)
    }

    private fun buildRequest(option: androidx.credentials.CredentialOption): GetCredentialRequest {
        return GetCredentialRequest.Builder().addCredentialOption(option).build()
    }
}

data class AuthenticatedAccount(
    val userId: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String?,
) {
    val displayLabel: String get() = displayName.ifBlank { email }
}

private data class CredentialRequestSpec(
    val request: GetCredentialRequest,
    val rawNonce: String,
)

private data class GoogleCredentialResult(
    val email: String,
    val displayName: String?,
    val profilePictureUri: String?,
    val idToken: String,
    val rawNonce: String,
)

private fun GetCredentialResponse.toGoogleCredential(rawNonce: String): GoogleCredentialResult {
    val cred = credential
    if (cred !is CustomCredential) error("Unexpected credential type: ${cred::class.java.simpleName}")
    if (cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        error("Unexpected custom credential type: ${cred.type}")
    }
    return try {
        val google = GoogleIdTokenCredential.createFrom(cred.data)
        GoogleCredentialResult(
            email = google.id,
            displayName = google.displayName,
            profilePictureUri = google.profilePictureUri?.toString(),
            idToken = google.idToken,
            rawNonce = rawNonce,
        )
    } catch (e: GoogleIdTokenParsingException) {
        throw IllegalStateException("Google ID token parsing failed.", e)
    }
}

private fun generateSecureRandomNonce(byteLength: Int = 32): String {
    val randomBytes = ByteArray(byteLength)
    SecureRandom().nextBytes(randomBytes)
    return Base64.encodeToString(randomBytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
}

private fun String.sha256Hex(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}
