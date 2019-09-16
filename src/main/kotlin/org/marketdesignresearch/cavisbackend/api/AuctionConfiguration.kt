package org.marketdesignresearch.cavisbackend.api

enum class PaymentRule { VCG, CCG }

data class CCAConfiguration(
        var supplementaryBids: Int = 10,
        var priceUpdate: Double = 0.1,
        var initialPriceUpdateIfPriceEqualsZero: Double = 1.0,
        var paymentRule: PaymentRule = PaymentRule.VCG,
        var maxRounds: Int = 100
)

data class PVMConfiguration(
        var initialRoundBids: Int = 20,
        var paymentRule: PaymentRule = PaymentRule.VCG,
        var maxRounds: Int = 100
)

data class AuctionConfiguration(
        var maxBids: Int = 20,
        var manualBids: Int = 2,
        var demandQueryTimeLimit: Double = 5.0,
        var reservePrices: Map<String, Double> = hashMapOf(),
        var useProposedReservePrices: Boolean = true,
        var ccaConfig: CCAConfiguration = CCAConfiguration(),
        var pvmConfig: PVMConfiguration = PVMConfiguration()
)