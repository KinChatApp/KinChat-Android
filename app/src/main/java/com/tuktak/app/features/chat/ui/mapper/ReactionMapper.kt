package com.tuktak.app.features.chat.ui.mapper

import com.tuktak.app.domain.model.ReactionType

object ReactionMapper {
    fun toEmoji(type: ReactionType): String {
        return when (type) {
            ReactionType.LIKE -> "👍"
            ReactionType.LOVE -> "❤️"
            ReactionType.LAUGH -> "😂"
            ReactionType.WOW -> "😮"
            ReactionType.SAD -> "😢"
            ReactionType.PRAY -> "🙏"
            ReactionType.UNKNOWN -> "👍"
        }
    }
}
