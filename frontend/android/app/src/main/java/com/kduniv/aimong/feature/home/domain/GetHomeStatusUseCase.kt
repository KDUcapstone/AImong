package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import javax.inject.Inject

class GetHomeStatusUseCase @Inject constructor(
    private val homeRepository: HomeRepository
) {
    suspend operator fun invoke(): Result<HomeScreenData> = homeRepository.getHome()
}
