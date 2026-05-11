package com.kduniv.aimong.feature.chat

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatHintNotifier @Inject constructor() {
    private val _hints = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val hints: SharedFlow<String> = _hints.asSharedFlow()

    fun offerHint(message: String) {
        val t = message.trim()
        if (t.isNotEmpty()) _hints.tryEmit(t)
    }
}
