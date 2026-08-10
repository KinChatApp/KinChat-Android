package com.kinchat.app.features.chat.ui

import com.kinchat.app.domain.model.ChatMessage
import com.kinchat.app.features.chat.ui.mapper.MessageUiMapper

object ChatItemsBuilder {

    fun build(
        messages: List<ChatMessage>,
        currentUserId: String,
        partnerName: String
    ): List<ChatListItem> {
        val result = mutableListOf<ChatListItem>()
        var lastDate: java.time.LocalDate? = null

        messages.forEachIndexed { index, msg ->
            val msgDate = localDateOf(msg.createdAt)
            if (msgDate != lastDate) {
                result.add(ChatListItem.Header(msgDate, dateLabelFor(msgDate)))
                lastDate = msgDate
            }

            val prev = messages.getOrNull(index - 1)
            val next = messages.getOrNull(index + 1)

            val uiModel = MessageUiMapper.mapToUiModel(
                entity = msg,
                currentUserId = currentUserId,
                partnerName = partnerName,
                isTopInGroup = !(prev != null && prev.senderId == msg.senderId && localDateOf(prev.createdAt) == msgDate),
                showTail = !(next != null && next.senderId == msg.senderId && localDateOf(next.createdAt) == msgDate),
                replyMessage = msg.replyToId?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { replyId -> messages.find { it.id == replyId } }
            )
            result.add(ChatListItem.Msg(uiModel))
        }

        return result
    }
}
