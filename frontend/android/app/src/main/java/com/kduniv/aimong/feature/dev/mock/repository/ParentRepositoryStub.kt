package com.kduniv.aimong.feature.dev.mock.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.network.model.ParentChildDetailData
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.ParentChildPatchResponseData
import com.kduniv.aimong.core.network.model.ParentMeData
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.core.network.model.PatchParentChildRequest
import com.kduniv.aimong.feature.parent.data.ParentRepository
import com.kduniv.aimong.feature.parent.data.model.CreateParentCustomQuestRequest
import com.kduniv.aimong.feature.parent.data.model.CreateParentCustomQuestResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentCustomQuestDto
import com.kduniv.aimong.feature.parent.data.model.ParentCustomQuestListResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentDailyStatDto
import com.kduniv.aimong.feature.parent.data.model.ParentStageMissionProgressDto
import com.kduniv.aimong.feature.parent.data.model.ParentStageRewardDto
import com.kduniv.aimong.feature.parent.data.model.ParentStageRewardsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointDto
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [com.kduniv.aimong.core.dev.UiMode.useStubNav] 전용 — 부모 대시보드·퀘스트·단계 보상 목업.
 */
@Singleton
class ParentRepositoryStub @Inject constructor(
    private val sessionManager: SessionManager,
    private val gson: Gson
) : ParentRepository {

    private val childListType = object : TypeToken<MutableList<ParentChildItem>>() {}.type
    private val stageRewardsByChild = mutableMapOf<String, MutableList<ParentStageRewardDto>>()
    private val customQuestsByChild = mutableMapOf<String, MutableList<ParentCustomQuestDto>>()

    override suspend fun syncParentChildren(): Result<List<ParentChildItem>> {
        val list = readChildrenFromCache()
        if (list.isEmpty()) {
            val seeded = seedDemoChild()
            saveChildren(seeded)
            return Result.success(seeded)
        }
        val linked = list.map { ensureLinkedForMock(it) }
        saveChildren(linked)
        return Result.success(linked)
    }

    override suspend fun getParentChildDetail(childId: String): Result<ParentChildDetailData> {
        val child = findChild(childId)
            ?: return Result.failure(IllegalStateException("자녀를 찾을 수 없습니다."))
        return Result.success(
            ParentChildDetailData(
                childId = child.childId,
                nickname = child.nickname,
                code = child.code,
                profileImageType = child.profileImageType,
                totalXp = child.totalXp,
                hasFcmToken = child.hasFcmToken,
                lastActiveAt = child.lastActiveAt ?: mockLastActiveAt(),
                createdAt = child.createdAt
            )
        )
    }

    override suspend fun getParentMe(): Result<ParentMeData> =
        Result.success(ParentMeData(email = "mock@aimong.dev", childrenCount = readChildrenFromCache().size))

    override suspend fun addParentChild(nickname: String): Result<ParentRegisterResponse> {
        val response = ParentRegisterResponse(
            childId = UUID.randomUUID().toString(),
            nickname = nickname.trim(),
            code = "482916",
            starterTickets = 3
        )
        val list = readChildrenFromCache().toMutableList()
        list.add(
            ParentChildItem(
                childId = response.childId,
                nickname = response.nickname,
                code = response.code,
                profileImageType = "SPROUT",
                totalXp = 150,
                hasFcmToken = true,
                lastActiveAt = mockLastActiveAt(),
                createdAt = mockLastActiveAt()
            )
        )
        saveChildren(list)
        return Result.success(response)
    }

    override suspend fun regenerateChildCode(childId: String): Result<String> {
        val newCode = (100000..999999).random().toString()
        val list = readChildrenFromCache().map {
            if (it.childId == childId) it.copy(code = newCode) else it
        }
        saveChildren(list)
        return Result.success(newCode)
    }

    override fun observeCachedParentChildren(): Flow<List<ParentChildItem>> =
        sessionManager.parentChildrenJson.map { json ->
            if (json.isNullOrBlank()) emptyList()
            else runCatching { gson.fromJson<List<ParentChildItem>>(json, childListType) }.getOrElse { emptyList() }
        }

    override suspend fun getChildSummary(childId: String): Result<ParentChildSummaryResponseData> {
        val child = findChild(childId) ?: return Result.failure(IllegalStateException("자녀 없음"))
        return Result.success(
            ParentChildSummaryResponseData(
                nickname = child.nickname,
                profileImageType = child.profileImageType,
                totalXp = child.totalXp.coerceAtLeast(150),
                continuousDays = 5,
                shieldCount = 2,
                weeklyCompletedSetCount = 7,
                totalCompletedSetCount = 12,
                currentLevelNo = 2,
                lastActiveAt = child.lastActiveAt ?: mockLastActiveAt()
            )
        )
    }

    override suspend fun getWeeklyStats(childId: String): Result<ParentWeeklyStatsResponseData> {
        findChild(childId) ?: return Result.failure(IllegalStateException("자녀 없음"))
        val zone = ZoneId.of("Asia/Seoul")
        val today = LocalDate.now(zone)
        val weekStart = today.with(java.time.DayOfWeek.MONDAY)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val counts = listOf(2, 0, 3, 1, 1, 0, 0)
        val xps = listOf(20, 0, 35, 10, 10, 0, 0)
        val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        val daily = (0..6).map { i ->
            ParentDailyStatDto(
                date = weekStart.plusDays(i.toLong()).format(formatter),
                dayOfWeek = days[i],
                completedSetCount = counts[i],
                xpEarned = xps[i]
            )
        }
        return Result.success(
            ParentWeeklyStatsResponseData(
                weekStart = weekStart.format(formatter),
                weekEnd = weekStart.plusDays(6).format(formatter),
                totalWeeklyXp = xps.sum(),
                totalWeeklyMissions = counts.sum(),
                dailyStats = daily
            )
        )
    }

    override suspend fun getWeakPoints(childId: String, page: Int, size: Int): Result<ParentWeakPointsResponseData> {
        findChild(childId) ?: return Result.failure(IllegalStateException("자녀 없음"))
        val items = listOf(
            ParentWeakPointDto(
                missionTitle = "팩트체크란?",
                setTitle = "팩트체크 판단하기",
                setId = "S0301-L4",
                levelNo = 4,
                difficulty = "MEDIUM",
                stage = 3,
                incorrectRate = 0.6,
                attemptCount = 5
            ),
            ParentWeakPointDto(
                missionTitle = "개인정보 보호",
                stage = 2,
                incorrectRate = 0.4,
                attemptCount = 3
            )
        )
        return Result.success(
            ParentWeakPointsResponseData(
                page = page,
                size = size,
                totalCount = items.size,
                hasNext = false,
                analyzedPeriod = "최근 30일",
                weakPoints = items
            )
        )
    }

    override suspend fun getCustomQuests(
        childId: String,
        status: String,
        page: Int?,
        size: Int?
    ): Result<ParentCustomQuestListResponseData> {
        findChild(childId) ?: return Result.failure(IllegalStateException("자녀 없음"))
        val all = customQuestsByChild.getOrPut(childId) { mutableListOf() }
        val filtered = when {
            status.contains("COMPLETED") || status.contains("CANCELLED") ->
                all.filter { it.status.equals("COMPLETED", true) || it.status.equals("CANCELLED", true) }
            else ->
                all.filter { it.status.equals("ACTIVE", true) || it.status.equals("PENDING_CONFIRM", true) }
        }.sortedByDescending { it.createdAt }
        val pageIndex = page ?: 0
        val pageSize = size ?: filtered.size.coerceAtLeast(1)
        val slice = filtered.drop(pageIndex * pageSize).take(pageSize)
        val hasNext = (pageIndex + 1) * pageSize < filtered.size
        return Result.success(
            ParentCustomQuestListResponseData(
                quests = slice,
                page = pageIndex,
                size = pageSize,
                totalCount = filtered.size,
                hasNext = hasNext
            )
        )
    }

    override suspend fun createCustomQuest(
        childId: String,
        body: CreateParentCustomQuestRequest
    ): Result<CreateParentCustomQuestResponseData> {
        findChild(childId) ?: return Result.failure(IllegalStateException("자녀 없음"))
        val list = customQuestsByChild.getOrPut(childId) { mutableListOf() }
        if (list.count { it.status == "ACTIVE" || it.status == "PENDING_CONFIRM" } >= 3) {
            return Result.failure(IllegalStateException("먼저 기존 퀘스트를 완료하거나 취소해주세요"))
        }
        val created = CreateParentCustomQuestResponseData(
            questId = UUID.randomUUID().toString(),
            status = "ACTIVE",
            title = body.title,
            rewardText = body.rewardText,
            expiresAt = body.expiresAt
        )
        list.add(
            0,
            ParentCustomQuestDto(
                questId = created.questId,
                title = created.title,
                description = body.description,
                rewardText = created.rewardText,
                status = created.status,
                expiresAt = created.expiresAt,
                createdAt = mockLastActiveAt()
            )
        )
        return Result.success(created)
    }

    override suspend fun confirmCustomQuest(questId: String): Result<Unit> {
        updateQuestStatus(questId, "COMPLETED")
        return Result.success(Unit)
    }

    override suspend fun cancelCustomQuest(questId: String): Result<Unit> {
        updateQuestStatus(questId, "CANCELLED")
        return Result.success(Unit)
    }

    override suspend fun getStageRewards(childId: String): Result<ParentStageRewardsResponseData> {
        findChild(childId) ?: return Result.failure(IllegalStateException("자녀 없음"))
        return Result.success(
            ParentStageRewardsResponseData(
                stages = stagesFor(childId).sortedBy { it.stageNumber }
            )
        )
    }

    override suspend fun saveStageReward(
        childId: String,
        stageNumber: Int,
        rewardText: String,
        hasExistingReward: Boolean
    ): Result<Unit> {
        findChild(childId) ?: return Result.failure(IllegalStateException("자녀 없음"))
        val list = stagesFor(childId)
        val index = list.indexOfFirst { it.stageNumber == stageNumber }
        if (index < 0) return Result.failure(IllegalStateException("단계를 찾을 수 없습니다."))
        if (list[index].isTriggered) {
            return Result.failure(IllegalStateException("이미 지급된 보상은 수정할 수 없습니다"))
        }
        list[index] = list[index].copy(rewardText = rewardText.trim())
        stageRewardsByChild[childId] = list
        return Result.success(Unit)
    }

    override suspend fun deleteParentFcmToken(firebaseIdToken: String): Result<Unit> = Result.success(Unit)

    override suspend fun parentLogout(firebaseIdToken: String): Result<Unit> = Result.success(Unit)

    override suspend fun patchParentChild(
        childId: String,
        body: PatchParentChildRequest
    ): Result<ParentChildPatchResponseData> {
        val child = findChild(childId) ?: return Result.failure(IllegalStateException("자녀 없음"))
        val nick = body.nickname?.trim().orEmpty().ifBlank { child.nickname }
        val list = readChildrenFromCache().map {
            if (it.childId == childId) it.copy(nickname = nick) else it
        }
        saveChildren(list)
        return Result.success(
            ParentChildPatchResponseData(
                childId = childId,
                nickname = nick,
                profileImageType = child.profileImageType
            )
        )
    }

    override suspend fun deleteParentChild(childId: String): Result<Unit> {
        val list = readChildrenFromCache().filter { it.childId != childId }
        saveChildren(list)
        stageRewardsByChild.remove(childId)
        customQuestsByChild.remove(childId)
        return Result.success(Unit)
    }

    override suspend fun deleteParentAccount(firebaseIdToken: String): Result<Unit> = Result.success(Unit)

    private fun stagesFor(childId: String): MutableList<ParentStageRewardDto> =
        stageRewardsByChild.getOrPut(childId) { defaultStages().toMutableList() }

    private fun defaultStages(): List<ParentStageRewardDto> = listOf(
        ParentStageRewardDto(
            stageNumber = 1,
            rewardText = null,
            isTriggered = false,
            defaultGearReward = 30,
            normalTicketReward = 0,
            missionProgress = ParentStageMissionProgressDto(completed = 3, total = 5)
        ),
        ParentStageRewardDto(
            stageNumber = 2,
            rewardText = null,
            isTriggered = false,
            defaultGearReward = 50,
            normalTicketReward = 0,
            missionProgress = ParentStageMissionProgressDto(completed = 1, total = 5)
        ),
        ParentStageRewardDto(
            stageNumber = 3,
            rewardText = null,
            isTriggered = false,
            defaultGearReward = 80,
            normalTicketReward = 3,
            missionProgress = ParentStageMissionProgressDto(completed = 0, total = 5)
        )
    )

    private fun updateQuestStatus(questId: String, status: String) {
        customQuestsByChild.values.forEach { list ->
            val i = list.indexOfFirst { it.questId == questId }
            if (i >= 0) {
                list[i] = list[i].copy(status = status)
            }
        }
    }

    private suspend fun readChildrenFromCache(): List<ParentChildItem> {
        val json = sessionManager.parentChildrenJson.first()
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { gson.fromJson<List<ParentChildItem>>(json, childListType) }.getOrElse { emptyList() }
    }

    private suspend fun saveChildren(list: List<ParentChildItem>) {
        sessionManager.saveParentChildrenJson(gson.toJson(list))
    }

    private suspend fun findChild(childId: String): ParentChildItem? =
        readChildrenFromCache().firstOrNull { it.childId == childId }

    private fun ensureLinkedForMock(item: ParentChildItem): ParentChildItem =
        if (!item.lastActiveAt.isNullOrBlank()) item
        else item.copy(
            lastActiveAt = mockLastActiveAt(),
            totalXp = item.totalXp.coerceAtLeast(150)
        )

    private fun seedDemoChild(): List<ParentChildItem> = listOf(
        ParentChildItem(
            childId = UUID.randomUUID().toString(),
            nickname = "김민준",
            code = "482916",
            profileImageType = "SPROUT",
            totalXp = 150,
            hasFcmToken = true,
            lastActiveAt = mockLastActiveAt(),
            createdAt = mockLastActiveAt()
        )
    )

    private fun mockLastActiveAt(): String =
        Instant.now().minus(2, ChronoUnit.HOURS).toString()
}
