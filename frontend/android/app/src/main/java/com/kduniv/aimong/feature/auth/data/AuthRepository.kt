package com.kduniv.aimong.feature.auth.data

import com.kduniv.aimong.core.network.model.ChildLoginResponse
import com.kduniv.aimong.core.network.model.ParentRegisterResponse

interface AuthRepository {
    suspend fun registerParentChild(nickname: String, firebaseIdToken: String): Result<ParentRegisterResponse>
    suspend fun loginChild(code: String): Result<ChildLoginResponse>
    /** Firebase ID 토큰(원문) + FCM 토큰. 실패해도 UI에 노출하지 않는 best-effort 용. */
    suspend fun registerParentFcmToken(firebaseIdToken: String, fcmToken: String): Result<Unit>
    /** Authorization 은 OkHttp 인터셉터(자녀 JWT). best-effort. */
    suspend fun registerChildFcmToken(fcmToken: String): Result<Unit>
}
