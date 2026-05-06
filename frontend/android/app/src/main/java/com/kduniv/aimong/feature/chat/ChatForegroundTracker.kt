package com.kduniv.aimong.feature.chat

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatForegroundTracker @Inject constructor() {
    @Volatile
    var isChatVisible: Boolean = false
}
