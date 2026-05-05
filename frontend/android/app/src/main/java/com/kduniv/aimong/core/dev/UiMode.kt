package com.kduniv.aimong.core.dev

/**
 * **앱 전체** «목업 네비» ↔ «실제 네비» 전환 — **`useStubNav` 한 줄만** 바꾸면 됩니다.
 *
 * | 값 | 인증·역할 | 자녀 탭 | 부모 |
 * |----|-----------|---------|------|
 * | `false` | `nav_main` (실제 Firebase/API) | `nav_child` | `nav_parent` |
 * | `true` | `nav_main_stub` — **실제와 동일 XML**의 [com.kduniv.aimong.feature.dev.mock] 목업 | `nav_child_stub` — 홈·학습은 동일 레이아웃 목업, 나머지는 실제 Fragment | `nav_parent_stub` — 실제 부모 Fragment(현재와 동일 UI) |
 *
 * @see com.kduniv.aimong.MainActivity
 */
object UiMode {
    const val useStubNav: Boolean = true
}
