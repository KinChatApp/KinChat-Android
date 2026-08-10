package com.kinchat.app.data.repository.chat.sync.utils

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

object SyncRetryHelper {
    private const val TAG = "SyncRetryHelper"

    /**
     * Executes the given [block] with a backoff retry mechanism.
     */
    suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 1000L,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Operation failed on attempt ${attempt + 1}, retrying in $currentDelay ms", e)
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong()
            }
        }
        // Final attempt without catching exception (will throw if fails)
        return block()
    }
}
