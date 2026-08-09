package com.kduniv.aimong.feature.gacha.data

import com.kduniv.aimong.feature.gacha.data.model.GachaExchangeData
import com.kduniv.aimong.feature.gacha.data.model.GachaFragmentsData
import com.kduniv.aimong.feature.gacha.data.model.GachaPullData

interface GachaRepository {
    /** v2.2: 항상 `ticketType=NORMAL` */
    suspend fun pull(): Result<GachaPullData>
    suspend fun getFragments(): Result<GachaFragmentsData>
    suspend fun exchange(grade: String, petType: String): Result<GachaExchangeData>
}
