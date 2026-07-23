package com.kinchat.app.data.repository

import com.kinchat.app.data.remote.redis.UpstashRedisApi
import com.kinchat.app.domain.repository.RedisRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RedisRepositoryImpl @Inject constructor(
    private val api: UpstashRedisApi
) : RedisRepository {

    override suspend fun getValue(key: String): Result<String?> {
        return try {
            val response = api.get(key)
            if (response.error == null) {
                Result.success(response.result)
            } else {
                Result.failure(Exception(response.error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setValue(key: String, value: String): Result<Boolean> {
        return try {
            val response = api.set(key, value)
            if (response.error == null) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
