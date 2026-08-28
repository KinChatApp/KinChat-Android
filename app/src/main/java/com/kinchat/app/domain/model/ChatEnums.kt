package com.kinchat.app.domain.model

enum class MessageType(val value: String) {
    TEXT("text"), IMAGE("image"), AUDIO("audio"), VIDEO("video"), DOCUMENT("document"), UNKNOWN("unknown");
    companion object { fun from(value: String?) = entries.find { it.value == value } ?: UNKNOWN }
}

enum class CallType(val value: String) {
    AUDIO("audio"), VIDEO("video"), UNKNOWN("unknown");
    companion object { fun from(value: String?) = entries.find { it.value == value } ?: UNKNOWN }
}

enum class CallStatus(val value: String) {
    RINGING("ringing"), ANSWERED("answered"), ENDED("ended"), MISSED("missed"),
    REJECTED("rejected"), CANCELLED("cancelled"), FAILED("failed"), UNKNOWN("unknown");
    companion object { fun from(value: String?) = entries.find { it.value == value } ?: UNKNOWN }
}

enum class ReactionType(val value: String) {
    LIKE("like"), LOVE("love"), LAUGH("laugh"), WOW("wow"), SAD("sad"), PRAY("pray"), UNKNOWN("unknown");
    companion object { fun from(value: String?) = entries.find { it.value == value } ?: UNKNOWN }
}

// 🚀 আপডেট: অফলাইন ফার্স্ট আর্কিটেকচারের জন্য FAILED স্টেট যুক্ত করা হলো
enum class TickState { SENDING, SENT, DELIVERED, READ, FAILED }
