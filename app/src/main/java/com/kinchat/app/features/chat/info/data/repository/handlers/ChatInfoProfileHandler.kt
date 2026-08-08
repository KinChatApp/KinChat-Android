package com.kinchat.app.features.chat.info.data.repository.handlers

import com.kinchat.app.data.local.db.UserDao
import com.kinchat.app.data.local.db.UserEntity
import com.kinchat.app.features.chat.info.domain.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatInfoProfileHandler @Inject constructor(
    private val supabase: SupabaseClient,
    private val userDao: UserDao
) {
    fun getUserProfile(userId: String): Flow<UserProfile?> = channelFlow {
        launch {
            try {
                val remoteData = supabase.postgrest["users"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<UserProfile>()
                
                if (remoteData != null) {
                    val entity = UserEntity(
                        id = remoteData.id,
                        displayName = remoteData.display_name ?: "Unknown",
                        username = null,
                        phone = remoteData.phone,
                        bio = remoteData.bio,
                        avatarUrl = remoteData.avatar_url,
                        isOnline = remoteData.is_online,
                        isVerified = false,
                        isDeleted = false,
                        lastSeen = null,
                        updatedAt = System.currentTimeMillis()
                    )
                    userDao.insertUser(entity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        launch {
            userDao.observeUser(userId).collect { localUser ->
                if (localUser != null) {
                    val profile = UserProfile(
                        id = localUser.id,
                        display_name = localUser.displayName,
                        avatar_url = localUser.avatarUrl,
                        phone = localUser.phone,
                        bio = localUser.bio,
                        is_online = localUser.isOnline,
                        last_seen = localUser.lastSeen?.toString()
                    )
                    send(profile)
                } else {
                    send(null)
                }
            }
        }

        val channel = supabase.channel("public:users:$userId")
        
        launch {
            try {
                val changeFlow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                    table = "users"
                    filter = "id=eq.$userId"
                }
                channel.subscribe()

                changeFlow.collect { action ->
                    action.record?.let { jsonElement ->
                        val updatedProfile = Json { ignoreUnknownKeys = true }.decodeFromJsonElement<UserProfile>(jsonElement)
                        val entity = UserEntity(
                            id = updatedProfile.id,
                            displayName = updatedProfile.display_name ?: "Unknown",
                            username = null,
                            phone = updatedProfile.phone,
                            bio = updatedProfile.bio,
                            avatarUrl = updatedProfile.avatar_url,
                            isOnline = updatedProfile.is_online,
                            isVerified = false,
                            isDeleted = false,
                            lastSeen = null,
                            updatedAt = System.currentTimeMillis()
                        )
                        userDao.insertUser(entity)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(NonCancellable) {
                    try {
                        channel.unsubscribe()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        awaitClose {
            // Suspends until flow is cancelled, allowing standard Coroutine flow cancellation.
        }
    }
}
