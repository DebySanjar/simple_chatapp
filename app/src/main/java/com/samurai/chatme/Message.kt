package com.samurai.chatme

import com.google.firebase.database.PropertyName


data class MessageModel(
    val message: String = "",
    val senderId: String = "",
    val senderName: String = "",

    val receiverId: String = "",

    val isSeen: Boolean = false, // O'qilgan yoki yo'q
)

