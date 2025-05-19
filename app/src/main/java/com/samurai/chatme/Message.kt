package com.samurai.chatme

import com.google.firebase.database.PropertyName


data class MessageModel(
    var message: String = "",
    var senderId: String = "",
    var senderName: String = "",

    var receiverId: String = "",

    var kordim: Boolean = false,
)

