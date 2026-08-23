package com.kinchat.app.data.repository.chat.sync.handlers

import com.kinchat.app.data.local.db.OperationType
import com.kinchat.app.data.local.db.PendingOperationEntity
import javax.inject.Inject

class PendingOperationDispatcher @Inject constructor(
    private val messageHandler: MessageOperationHandler,
    private val reactionHandler: ReactionOperationHandler,
    private val participantHandler: ParticipantOperationHandler,
    private val authHandler: AuthOperationHandler,
    private val attachmentHandler: AttachmentOperationHandler
) {
    suspend fun dispatch(op: PendingOperationEntity) {
        when (op.type) {
            OperationType.SEND_MESSAGE,
            OperationType.EDIT_MESSAGE,
            OperationType.DELETE_MESSAGE -> messageHandler.handle(op)

            OperationType.ADD_REACTION,
            OperationType.REMOVE_REACTION -> reactionHandler.handle(op)

            OperationType.UPDATE_CHAT_PIN,
            OperationType.UPDATE_CHAT_MUTE,
            OperationType.UPDATE_CHAT_ARCHIVE,
            OperationType.UPDATE_CHAT_HIDDEN,
            OperationType.UPDATE_LAST_READ,
            OperationType.CREATE_CHAT,           // 🚀 FIX: چ্যাট ক্রিয়েট অপারেশন যোগ করা হলো
            OperationType.UPDATE_CHAT_FAVORITE,  // 🚀 FIX: মিসিং অপারেশন যোগ করা হলো
            OperationType.UPDATE_CHAT_BLOCK,     // 🚀 FIX: মিসিং অপারেশন যোগ করা হলো
            OperationType.REPORT_MESSAGE -> participantHandler.handle(op)

            OperationType.UPDATE_FCM_TOKEN -> authHandler.handle(op)

            OperationType.UPLOAD_ATTACHMENT -> attachmentHandler.handle(op)

            else -> {
                // Operation type unhandled. The operation falls through
                // and gets safely deleted by the worker loop so it doesn't block the queue.
            }
        }
    }
}
