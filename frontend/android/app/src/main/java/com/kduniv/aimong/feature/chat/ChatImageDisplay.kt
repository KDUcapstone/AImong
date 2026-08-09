package com.kduniv.aimong.feature.chat

import com.kduniv.aimong.core.network.ChatGeneratedImageDto

/** `data:{mimeType};base64,{b64Json}` — Glide·ImageView 로 표시 */
fun ChatGeneratedImageDto.toDataUri(): String? {
    val payload = b64Json.trim()
    if (payload.isEmpty()) return null
    val mime = mimeType.trim().ifEmpty { "image/png" }
    return "data:$mime;base64,$payload"
}
