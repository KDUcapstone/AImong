package com.kduniv.aimong.core.privacy

import javax.inject.Inject
import javax.inject.Singleton

/** [com.kduniv.aimong.core.dev.UiMode.useStubNav] 전용 — `privacy/event` API 호출 없음. */
@Singleton
class PrivacyRepositoryStub @Inject constructor() : PrivacyRepository {
    override suspend fun reportEvent(detectedType: String, masked: Boolean) {
        // no-op
    }
}
