package com.tuktak.app.domain.repository

interface RedisRepository {
    suspend fun getValue(key: String): Result<String?>
    suspend fun setValue(key: String, value: String): Result<Boolean>
}
