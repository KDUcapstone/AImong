package com.kduniv.aimong.core.privacy

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyRadar @Inject constructor() {
    // 2차 필터: Regex (명세서 7-2 반영)
    private val patterns = listOf(
        Regex("""[가-힣]{2,4}(이야|입니다|야|이에요|예요)"""), // 이름
        Regex("""(초등학교|중학교|고등학교|\w+초|\w+중|\w+고)"""), // 학교
        Regex("""\d+살|\d+세"""), // 나이
        Regex("""\d+학년""") // 학년
    )

    /**
     * 입력된 텍스트에 개인정보가 포함되어 있는지 검사합니다.
     * @return 개인정보가 감지되면 true, 아니면 false
     */
    fun checkPrivacy(text: String): Boolean {
        // patterns 중 하나라도 매칭되면 true 반환
        return patterns.any { it.containsMatchIn(text) }
    }

    /**
     * 감지된 개인정보의 타입을 반환합니다.
     */
    fun detectPrivacyType(text: String): PrivacyType {
        if (Regex("""(초등학교|중학교|고등학교|\w+초|\w+중|\w+고)""").containsMatchIn(text)) return PrivacyType.SCHOOL
        if (Regex("""\d+살|\d+세""").containsMatchIn(text)) return PrivacyType.AGE
        // ... 추가적인 정밀 판정 로직
        return PrivacyType.ETC
    }
}
