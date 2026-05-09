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

    private val patterns = listOf(
        Regex("""[가-힣]{2,4}(이야|입니다|야|이에요|예요)"""),
        Regex("""(초등학교|중학교|고등학교|\w+초|\w+중|\w+고)"""),
        Regex("""\d+살|\d+세"""),
        Regex("""\d+학년"""),
        Regex("""010[- .]?\d{3,4}[- .]?\d{4}"""),
        Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
    )

    private val maskPlaceholder = "[***]"

    /**
     * 채팅 입력 검사·UI 하이라이트용: 병합된 민감 구간(문자 인덱스) 목록.
     */
    suspend fun scanSensitiveRangesForChat(text: String): List<IntRange> =
        mergeRanges(collectSensitiveRanges(text))

    /**
     * 채팅 전송용: 개인정보 구간을 마스킹한 문자열과, 치환 여부를 반환합니다.
     */
    suspend fun maskForChatSend(text: String): Pair<String, Boolean> {
        val merged = mergeRanges(collectSensitiveRanges(text))
        if (merged.isEmpty()) return text to false

        var result = text
        for (range in merged.sortedByDescending { it.first }) {
            result = result.replaceRange(range, maskPlaceholder)
        }
        return result to true
    }

    private suspend fun collectSensitiveRanges(text: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()

        try {
            entityExtractor.downloadModelIfNeeded().await()
            val annotations = entityExtractor.annotate(text).await()
            for (annotation in annotations) {
                val sensitive = annotation.entities.any { entity ->
                    entity.type == Entity.TYPE_PHONE ||
                        entity.type == Entity.TYPE_EMAIL ||
                        entity.type == Entity.TYPE_ADDRESS ||
                        entity.type == Entity.TYPE_URL ||
                        entity.type == Entity.TYPE_DATE_TIME
                }
                if (!sensitive) continue
                val start = annotation.start
                val end = annotation.end
                if (start in 0..text.length && end in start..text.length) {
                    ranges.add(start until end)
                }
            }
        } catch (_: Exception) {
            // ML Kit 실패 시 Regex만 사용
        }

        for (pattern in patterns) {
            pattern.findAll(text).forEach { match ->
                ranges.add(match.range)
            }
        }

        return ranges
    }

    private fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return emptyList()
        val sorted = ranges.sortedBy { it.first }
        val out = mutableListOf<IntRange>()
        var cur = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            cur = if (next.first <= cur.last + 1) {
                cur.first..maxOf(cur.last, next.last)
            } else {
                out.add(cur)
                next
            }
        }
        out.add(cur)
        return out
    }

    suspend fun checkPrivacy(text: String): Boolean =
        scanSensitiveRangesForChat(text).isNotEmpty()

    fun detectPrivacyType(text: String): PrivacyType {
        if (Regex("""(초등학교|중학교|고등학교|\w+초|\w+중|\w+고)""").containsMatchIn(text)) return PrivacyType.SCHOOL
        if (Regex("""\d+살|\d+세""").containsMatchIn(text)) return PrivacyType.AGE
        if (Regex("""\d+학년""").containsMatchIn(text)) return PrivacyType.ETC
        if (Regex("""010[- .]?\d{3,4}[- .]?\d{4}""").containsMatchIn(text)) return PrivacyType.PHONE
        if (Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""").containsMatchIn(text)) return PrivacyType.EMAIL
        if (Regex("""[가-힣]{2,4}(이야|입니다|야|이에요|예요)""").containsMatchIn(text)) return PrivacyType.NAME
        return PrivacyType.ETC
    }
}
