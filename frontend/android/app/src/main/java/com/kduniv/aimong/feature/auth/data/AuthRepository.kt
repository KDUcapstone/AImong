package com.kduniv.aimong.feature.auth.data

import com.kduniv.aimong.core.network.model.ChildLogoutResponse
import com.kduniv.aimong.core.network.model.ChildMeResponseData
import com.kduniv.aimong.core.network.model.ChildLoginResponse
import com.kduniv.aimong.core.network.model.ParentRegisterResponse

interface AuthRepository {
    suspend fun registerParentChild(nickname: String, firebaseIdToken: String): Result<ParentRegisterResponse>
    suspend fun loginChild(code: String): Result<ChildLoginResponse>
    /** Firebase ID 토큰(원문) + FCM 토큰. 실패해도 UI에 노출하지 않는 best-effort 용. */
    suspend fun registerParentFcmToken(firebaseIdToken: String, fcmToken: String): Result<Unit>
    /** Authorization 은 OkHttp 인터셉터(자녀 JWT). best-effort. */
    suspend fun registerChildFcmToken(fcmToken: String): Result<Unit>

    /** 자녀 세션 유효성 확인 */
    suspend fun getChildMe(): Result<ChildMeResponseData>

    /** 자녀 로그아웃 (서버 세션/FCM 정리용) */
    suspend fun logoutChild(): Result<ChildLogoutResponse>

    /** 자녀 FCM 토큰 해제. 실패해도 UX에 노출하지 않는 best-effort. */
    suspend fun deleteChildFcmToken(): Result<Unit>
}
