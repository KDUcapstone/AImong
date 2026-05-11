package com.kduniv.aimong.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** v2.3 미션 맵 캐시 — 소단원 단위 */
@Entity(tableName = "mission_chapters")
data class MissionChapterEntity(
    @PrimaryKey val missionId: String,
    val missionCode: String,
    val stage: Int,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    /** Gson List<MissionStarLevelSnapshot> */
    val starLevelsJson: String
)
