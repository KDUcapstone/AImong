package com.kduniv.aimong.feature.parent.domain

object ParentAuthPolicy {
    /** v2.2: 부모당 등록 가능 자녀 수 */
    const val MAX_CHILDREN = 3

    /** v2.3: 온보딩·자녀 추가 시 NORMAL 기본 티켓 지급 수(서버 정책과 동일) */
    const val STARTER_TICKETS = 3
}
