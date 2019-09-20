package org.marketdesignresearch.cavisbackend.domains

import org.marketdesignresearch.cavisbackend.api.AuctionConfiguration
import org.marketdesignresearch.cavisbackend.api.PaymentRule
import org.marketdesignresearch.mechlib.core.Domain
import org.marketdesignresearch.mechlib.core.Good
import org.marketdesignresearch.mechlib.core.price.LinearPrices
import org.marketdesignresearch.mechlib.core.price.Price
import org.marketdesignresearch.mechlib.mechanism.auctions.Auction
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCAuction
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.bidcollection.supplementaryround.ProfitMaximizingSupplementaryRound
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.priceupdate.SimpleRelativePriceUpdate
import org.marketdesignresearch.mechlib.mechanism.auctions.pvm.PVMAuction
import org.marketdesignresearch.mechlib.mechanism.auctions.pvm.ml.MLAlgorithm
import org.marketdesignresearch.mechlib.mechanism.auctions.sequential.SequentialAuction
import org.marketdesignresearch.mechlib.outcomerules.OutcomeRuleGenerator
import java.math.BigDecimal
import kotlin.math.max

enum class AuctionFactory(val prettyName: String) {
    SINGLE_ITEM_FIRST_PRICE("Single Item, First Price"),
    SINGLE_ITEM_SECOND_PRICE("Single Item, Second Price"),
    SEQUENTIAL_FIRST_PRICE("Sequential, First Price"),
    SEQUENTIAL_SECOND_PRICE("Sequential, Second Price"),
    SIMULTANEOUS_FIRST_PRICE("Simultaneous, First Price"),
    SIMULTANEOUS_SECOND_PRICE("Simultaneous, Second Price"),
    VCG("VCG"),
    CCA("CCA"),
    PVM("PVM");

    fun getAuction(domain: Domain, config: AuctionConfiguration = AuctionConfiguration()): Auction {
        val auction = when (this) {
            SINGLE_ITEM_FIRST_PRICE, SIMULTANEOUS_FIRST_PRICE -> Auction(domain, OutcomeRuleGenerator.FIRST_PRICE)
            SINGLE_ITEM_SECOND_PRICE, SIMULTANEOUS_SECOND_PRICE -> Auction(domain, OutcomeRuleGenerator.SECOND_PRICE)
            SEQUENTIAL_FIRST_PRICE -> SequentialAuction(domain, OutcomeRuleGenerator.FIRST_PRICE)
            SEQUENTIAL_SECOND_PRICE -> SequentialAuction(domain, OutcomeRuleGenerator.SECOND_PRICE)
            VCG -> Auction(domain, OutcomeRuleGenerator.VCG_XOR)
            CCA -> {
                val cca: CCAuction
                val outcomeRuleGenerator = when (config.ccaConfig.paymentRule) {
                    PaymentRule.VCG -> OutcomeRuleGenerator.VCG_XOR
                    PaymentRule.CCG -> OutcomeRuleGenerator.CCG
                }
                if (config.reservePrices.isNotEmpty()) {
                    val priceMap = hashMapOf<Good, Price>()
                    config.reservePrices.forEach { priceMap[domain.getGood(it.key)] = Price.of(it.value) }
                    cca = CCAuction(domain, outcomeRuleGenerator, LinearPrices(priceMap))
                } else {
                    cca = CCAuction(domain, outcomeRuleGenerator, config.useProposedReservePrices)
                }
                cca.setPriceUpdater(SimpleRelativePriceUpdate().withPriceUpdate(BigDecimal.valueOf(config.ccaConfig.priceUpdate)).withInitialUpdate(BigDecimal.valueOf(config.ccaConfig.initialPriceUpdateIfPriceEqualsZero)))
                cca.addSupplementaryRound(ProfitMaximizingSupplementaryRound().withNumberOfSupplementaryBids(config.ccaConfig.supplementaryBids))
                cca.maxRounds = config.ccaConfig.maxRounds
                return cca
            }
            PVM -> {
                val outcomeRuleGenerator = when (config.pvmConfig.paymentRule) {
                    PaymentRule.VCG -> OutcomeRuleGenerator.VCG_XOR
                    PaymentRule.CCG -> OutcomeRuleGenerator.CCG
                }
                val pvm = PVMAuction(domain, MLAlgorithm.Type.LINEAR_REGRESSION, outcomeRuleGenerator, max(domain.goods.size + 1, config.pvmConfig.initialRoundBids))
                pvm.maxRounds = config.pvmConfig.maxRounds
                return pvm
            }
        }
        auction.maxBids = config.maxBids
        auction.manualBids = config.manualBids
        auction.demandQueryTimeLimit = config.demandQueryTimeLimit
        return auction
    }
}
