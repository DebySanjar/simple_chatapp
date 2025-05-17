package com.samurai.chatme

import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName

data class User(
    val uid: String = "",
    val name: String = "",

    @get:PropertyName("photo")
    @set:PropertyName("photo")
    var imageUrl: String? = null,

    @get:Exclude var unreadCount: Int = 1
)
