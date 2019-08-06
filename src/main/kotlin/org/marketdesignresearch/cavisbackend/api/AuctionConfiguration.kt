package org.marketdesignresearch.cavisbackend.api

data class CCAConfiguration(
        val supplementaryBids: Int = 10,
        val priceUpdate: Double = 0.1
)

data class PVMConfiguration(
        val initialRoundBids: Int = 5
)

data class AuctionConfiguration(
        val maxBids: Int = 10,
        val demandQueryTimeLimit: Double = 5.0,
        val reservePrices: Map<String, Double> = hashMapOf(),
        val useProposedReservePrices: Boolean = true,
        val ccaConfig: CCAConfiguration = CCAConfiguration(),
        val pvmConfig: PVMConfiguration = PVMConfiguration()
)