package com.kinchat.app.features.chat.ui.utils

import com.kinchat.app.domain.model.ChatMessage
import java.time.Duration
import java.time.Instant

object MessagePermissions {

    fun canEdit(selected: List<ChatMessage>, currentUserId: String): Boolean {
        if (selected.size != 1) return false
        val msg = selected.first()
        return msg.senderId == currentUserId && withinMinutes(msg.createdAt, 15)
    }

    fun canDeleteForEveryone(selected: List<ChatMessage>, currentUserId: String): Boolean {
        if (selected.isEmpty()) return false
        return selected.all { msg ->
            msg.senderId == currentUserId && withinHours(msg.createdAt, 48)
        }
    }

    private fun withinMinutes(createdAt: String?, minutes: Long): Boolean {
        return createdAt?.let {
            try { Duration.between(Instant.parse(it), Instant.now()).toMinutes() <= minutes }
            catch (e: Exception) { false }
        } == true
    }

    private fun withinHours(createdAt: String?, hours: Long): Boolean {
        return createdAt?.let {
            try { Duration.between(Instant.parse(it), Instant.now()).toHours() <= hours }
            catch (e: Exception) { false }
        } == true
    }
}
