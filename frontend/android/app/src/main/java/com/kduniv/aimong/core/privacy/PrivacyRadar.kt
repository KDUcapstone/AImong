package com.kduniv.aimong.core.privacy

import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyRadar @Inject constructor() {
    private val entityExtractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.KOREAN).build()
    )

    // 2차 필터: Regex (명세서 7-2 반영)
    private val patterns = listOf(
        Regex("""[가-힣]{2,4}(이야|입니다|야|이에요|예요)"""), // 이름
        Regex("""(초등학교|중학교|고등학교|\w+초|\w+중|\w+고)"""), // 학교
        Regex("""\d+살|\d+세"""), // 나이
        Regex("""\d+학년"""), // 학년
        Regex("""010[- .]?\d{3,4}[- .]?\d{4}"""), // 전화번호
        Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""") // 이메일
    )

    /**
     * 입력된 텍스트에 개인정보가 포함되어 있는지 검사합니다.
     * @return 개인정보가 감지되면 true, 아니면 false
     */
    suspend fun checkPrivacy(text: String): Boolean {
        // 1. ML Kit Entity Extraction (1차 필터)
        try {
            entityExtractor.downloadModelIfNeeded().await()
            val entities = entityExtractor.annotate(text).await()
            if (entities.isNotEmpty()) {
                val hasSensitiveEntity = entities.any { annotation ->
                    annotation.entities.any { entity ->
                        entity.type == Entity.TYPE_PHONE ||
                        entity.type == Entity.TYPE_EMAIL ||
                        entity.type == Entity.TYPE_ADDRESS ||
                        entity.type == Entity.TYPE_URL ||
                        entity.type == Entity.TYPE_DATE_TIME
                    }
                }
                if (hasSensitiveEntity) return true
            }
        } catch (e: Exception) {
            // ML Kit 실패 시 Regex로 폴백
        }

        // 2. Regex 필터 (2차 필터)
        return patterns.any { it.containsMatchIn(text) }
    }

    /**
     * 감지된 개인정보의 타입을 반환합니다.
     */
    fun detectPrivacyType(text: String): PrivacyType {
        if (Regex("""(초등학교|중학교|고등학교|\w+초|\w+중|\w+고)""").containsMatchIn(text)) return PrivacyType.SCHOOL
        if (Regex("""\d+살|\d+세""").containsMatchIn(text)) return PrivacyType.AGE
        if (Regex("""\d+학년""").containsMatchIn(text)) return PrivacyType.ETC // 학년은 ETC로 분류하거나 추가 정의 가능
        if (Regex("""010[- .]?\d{3,4}[- .]?\d{4}""").containsMatchIn(text)) return PrivacyType.PHONE
        if (Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""").containsMatchIn(text)) return PrivacyType.EMAIL
        if (Regex("""[가-힣]{2,4}(이야|입니다|야|이에요|예요)""").containsMatchIn(text)) return PrivacyType.NAME

        return PrivacyType.ETC
    }
}
