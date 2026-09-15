package com.finora.android.domain.model

data class NetWorthAssetItem(
    val id: String,
    val name: String,
    val type: String, // "Bank Account", "Cash", "Digital Wallet", "Savings Goal"
    val balanceMinorUnits: Long,
    val percentageOfAssets: Double
)

data class NetWorthLiabilityItem(
    val id: String,
    val name: String,
    val type: String, // "Credit Card", "Loan"
    val balanceMinorUnits: Long,
    val percentageOfLiabilities: Double
)

data class NetWorthSummary(
    val totalNetWorthMinorUnits: Long,
    val totalAssetsMinorUnits: Long,
    val totalLiabilitiesMinorUnits: Long,
    val leverageRatioPercent: Double,
    val isLeverageConservative: Boolean,
    val assets: List<NetWorthAssetItem>,
    val liabilities: List<NetWorthLiabilityItem>
)

object NetWorthCalculation {

    fun calculate(
        liquidAccounts: List<AccountBalanceItem>,
        savingsGoalBalances: Long
    ): NetWorthSummary {
        val assetAccounts = mutableListOf<NetWorthAssetItem>()
        val liabilityAccounts = mutableListOf<NetWorthLiabilityItem>()

        var totalAssets = 0L
        var totalLiabilities = 0L

        // Process account items
        liquidAccounts.forEach { item ->
            if (item.type.equals("CREDIT_CARD", ignoreCase = true) || item.balanceMinorUnits < 0) {
                val liabilityAmount = kotlin.math.abs(item.balanceMinorUnits)
                totalLiabilities += liabilityAmount
                liabilityAccounts.add(
                    NetWorthLiabilityItem(
                        id = item.id,
                        name = item.name,
                        type = "Credit Card",
                        balanceMinorUnits = liabilityAmount,
                        percentageOfLiabilities = 0.0
                    )
                )
            } else {
                totalAssets += item.balanceMinorUnits
                assetAccounts.add(
                    NetWorthAssetItem(
                        id = item.id,
                        name = item.name,
                        type = item.type.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        balanceMinorUnits = item.balanceMinorUnits,
                        percentageOfAssets = 0.0
                    )
                )
            }
        }

        // Add savings goals to assets
        if (savingsGoalBalances > 0) {
            totalAssets += savingsGoalBalances
            assetAccounts.add(
                NetWorthAssetItem(
                    id = "savings_aggregate",
                    name = "Savings & Goals Vault",
                    type = "Savings Reserve",
                    balanceMinorUnits = savingsGoalBalances,
                    percentageOfAssets = 0.0
                )
            )
        }

        // Calculate allocation percentages
        val finalAssets = assetAccounts.map {
            val pct = if (totalAssets > 0) (it.balanceMinorUnits.toDouble() / totalAssets) * 100 else 0.0
            it.copy(percentageOfAssets = pct)
        }

        val finalLiabilities = liabilityAccounts.map {
            val pct = if (totalLiabilities > 0) (it.balanceMinorUnits.toDouble() / totalLiabilities) * 100 else 0.0
            it.copy(percentageOfLiabilities = pct)
        }

        val netWorth = totalAssets - totalLiabilities
        val leverageRatio = if (totalAssets > 0) (totalLiabilities.toDouble() / totalAssets) * 100 else 0.0

        return NetWorthSummary(
            totalNetWorthMinorUnits = netWorth,
            totalAssetsMinorUnits = totalAssets,
            totalLiabilitiesMinorUnits = totalLiabilities,
            leverageRatioPercent = leverageRatio,
            isLeverageConservative = leverageRatio < 25.0,
            assets = finalAssets,
            liabilities = finalLiabilities
        )
    }
}

data class AccountBalanceItem(
    val id: String,
    val name: String,
    val type: String,
    val balanceMinorUnits: Long
)
