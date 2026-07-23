package com.kinchat.app.data.remote.redis

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body
import kotlinx.serialization.Serializable

@Serializable
data class RedisResponse<T>(
    val result: T? = null,
    val error: String? = null
)

interface UpstashRedisApi {
    @GET("get/{key}")
    suspend fun get(@Path("key") key: String): RedisResponse<String>

    @POST("set/{key}")
    suspend fun set(@Path("key") key: String, @Body value: String): RedisResponse<String>
}
