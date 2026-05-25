package com.kduniv.aimong.feature.home.presentation.quest

import android.content.Context
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.quest.data.model.ChildCustomQuestDto
import com.kduniv.aimong.feature.quest.data.model.QuestApiItemDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object QuestSheetMapper {

    private val expiresDisplayFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    fun mapItem(
        dto: QuestApiItemDto,
        period: QuestSheetPeriod,
        canStartMission: Boolean
    ): QuestSheetRow {
        val claimType = dto.claimType.uppercase()
        val lineComplete = when (claimType) {
            "MANUAL" -> dto.completed && dto.rewardClaimed
            else -> dto.completed
        }
        val progressStr = "${dto.progress.current} / ${dto.progress.required}"
        val detailText = buildString {
            append(progressStr)
            if (dto.reward.isNotBlank()) {
                append('\n')
                append(dto.reward)
            }
        }
        // MISSION_*: 일반 모드 setId 통과 수 기준(복습 제외) — BE 집계
        val missionLike = dto.questType.contains("MISSION", ignoreCase = true)
        val chatLike = dto.questType.contains("CHAT", ignoreCase = true)

        val action: Pair<QuestSheetPrimaryAction, Boolean> = when {
            lineComplete -> QuestSheetPrimaryAction.COMPLETED to false
            claimType == "MANUAL" && dto.completed && !dto.rewardClaimed ->
                QuestSheetPrimaryAction.CLAIM to true
            !dto.completed && missionLike ->
                if (canStartMission) QuestSheetPrimaryAction.GO_LEARN to true
                else QuestSheetPrimaryAction.IN_PROGRESS to false
            !dto.completed && chatLike -> QuestSheetPrimaryAction.GO_CHAT to true
            else -> QuestSheetPrimaryAction.IN_PROGRESS to false
        }

        val showDot = action.first == QuestSheetPrimaryAction.CLAIM && action.second

        return QuestSheetRow(
            questType = dto.questType,
            title = dto.label,
            detailText = detailText,
            period = period,
            primaryAction = action.first,
            actionEnabled = action.second,
            showNotificationDot = showDot,
        )
    }

    fun mapCustomQuest(dto: ChildCustomQuestDto, context: Context): QuestSheetRow {
        val detailText = buildString {
            dto.description?.trim()?.takeIf { it.isNotEmpty() }?.let {
                append(it)
                append('\n')
            }
            append(context.getString(R.string.child_custom_quest_reward_fmt, dto.rewardText))
            formatExpires(dto.expiresAt)?.let { label ->
                append('\n')
                append(context.getString(R.string.child_custom_quest_expires_fmt, label))
            }
        }
        val (action, enabled) = when (dto.status.uppercase()) {
            "ACTIVE" -> QuestSheetPrimaryAction.COMPLETE_CUSTOM to true
            "PENDING_CONFIRM" -> QuestSheetPrimaryAction.AWAITING_CONFIRM to false
            "COMPLETED", "AUTO_CONFIRMED" -> QuestSheetPrimaryAction.COMPLETED to false
            else -> QuestSheetPrimaryAction.IN_PROGRESS to false
        }
        val showDot = action == QuestSheetPrimaryAction.COMPLETE_CUSTOM && enabled

        return QuestSheetRow(
            questType = dto.questId,
            title = dto.title,
            detailText = detailText,
            period = QuestSheetPeriod.PARENT,
            primaryAction = action,
            actionEnabled = enabled,
            isCustomQuest = true,
            showNotificationDot = showDot,
        )
    }

    private fun formatExpires(expiresAt: String?): String? {
        val raw = expiresAt?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return try {
            Instant.parse(raw).atZone(ZoneId.systemDefault()).format(expiresDisplayFormatter)
        } catch (_: Exception) {
            null
        }
    }
}
