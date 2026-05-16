package com.kduniv.aimong.feature.wallet.domain.repository

import com.kduniv.aimong.feature.wallet.domain.model.WalletBalance

interface WalletRepository {
    suspend fun getWallet(): Result<WalletBalance>
}
