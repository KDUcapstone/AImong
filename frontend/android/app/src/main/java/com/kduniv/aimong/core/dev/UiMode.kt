package com.kduniv.aimong.core.dev

/**
 * **앱 전체** «목업 네비» ↔ «실제 네비» 전환 — **`useStubNav` 한 줄만** 바꾸면 됩니다.
 *
 * | 값 | 인증·역할 | 자녀 탭 | 부모 |
 * |----|-----------|---------|------|
 * | `false` | `nav_main` (실제 Firebase/API) | `nav_child` | `nav_parent` |
 * | `true` | `nav_main_stub` — **실제와 동일 XML**의 [com.kduniv.aimong.feature.dev.mock] 목업 | `nav_child_stub` — 홈은 목업, 퀴즈·퀘스트·챗·가챠·펫은 [StubRepositoryModule]로 **로컬 스텁**(Retrofit 미호출) | `nav_parent_stub` — 실제 부모 Fragment(API 호출) |
 *
 * @see com.kduniv.aimong.MainActivity
 */
object UiMode {
    /** `true`: 목업 네비 + 퀴즈/퀘스트 등 Retrofit 스텁([com.kduniv.aimong.core.di.StubRepositoryModule]). `false`: 전부 실제 네비·API. */
    const val useStubNav: Boolean = true
}
