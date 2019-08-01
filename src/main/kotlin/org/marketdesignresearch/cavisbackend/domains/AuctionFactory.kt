package org.marketdesignresearch.cavisbackend.domains

import org.marketdesignresearch.cavisbackend.server.AuctionConfiguration
import org.marketdesignresearch.mechlib.core.Domain
import org.marketdesignresearch.mechlib.core.Good
import org.marketdesignresearch.mechlib.core.price.LinearPrices
import org.marketdesignresearch.mechlib.core.price.Price
import org.marketdesignresearch.mechlib.mechanism.auctions.Auction
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCAuction
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.bidcollection.supplementaryround.ProfitMaximizingSupplementaryRound
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.priceupdate.SimpleRelativePriceUpdate
import org.marketdesignresearch.mechlib.mechanism.auctions.pvm.PVMAuction
import org.marketdesignresearch.mechlib.mechanism.auctions.sequential.SequentialAuction
import org.marketdesignresearch.mechlib.outcomerules.OutcomeRuleGenerator
import java.math.BigDecimal

enum class AuctionFactory {
    SINGLE_ITEM_FIRST_PRICE,
    SINGLE_ITEM_SECOND_PRICE,
    SEQUENTIAL_FIRST_PRICE,
    SEQUENTIAL_SECOND_PRICE,
    SIMULTANEOUS_FIRST_PRICE,
    SIMULTANEOUS_SECOND_PRICE,
    VCG_XOR,
    VCG_OR,
    CCA_VCG,
    CCA_CCG,
    PVM_VCG,
    PVM_CCG;

    fun getAuction(domain: Domain, config: AuctionConfiguration = AuctionConfiguration()): Auction {
        val auction = when (this) {
            SINGLE_ITEM_FIRST_PRICE, SIMULTANEOUS_FIRST_PRICE -> Auction(domain, OutcomeRuleGenerator.FIRST_PRICE)
            SINGLE_ITEM_SECOND_PRICE, SIMULTANEOUS_SECOND_PRICE -> Auction(domain, OutcomeRuleGenerator.SECOND_PRICE)
            SEQUENTIAL_FIRST_PRICE -> SequentialAuction(domain, OutcomeRuleGenerator.FIRST_PRICE)
            SEQUENTIAL_SECOND_PRICE -> SequentialAuction(domain, OutcomeRuleGenerator.SECOND_PRICE)
            VCG_XOR -> Auction(domain, OutcomeRuleGenerator.VCG_XOR)
            VCG_OR -> Auction(domain, OutcomeRuleGenerator.VCG_OR)
            CCA_VCG -> {
                val cca: CCAuction
                if (config.reservePrices.isNotEmpty()) {
                    val priceMap = hashMapOf<Good, Price>()
                    config.reservePrices.forEach { priceMap[domain.getGood(it.key)] = Price.of(it.value) }
                    cca = CCAuction(domain, OutcomeRuleGenerator.VCG_XOR, LinearPrices(priceMap))
                } else {
                    cca = CCAuction(domain, OutcomeRuleGenerator.VCG_XOR, config.useProposedReservePrices)
                }
                cca.setPriceUpdater(SimpleRelativePriceUpdate().withPriceUpdate(BigDecimal.valueOf(config.ccaConfig.priceUpdate)))
                cca.addSupplementaryRound(ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(config.ccaConfig.supplementaryBids))
                return cca
            }
            CCA_CCG -> {
                val cca: CCAuction
                if (config.reservePrices.isNotEmpty()) {
                    val priceMap = hashMapOf<Good, Price>()
                    config.reservePrices.forEach { priceMap[domain.getGood(it.key)] = Price.of(it.value) }
                    cca = CCAuction(domain, OutcomeRuleGenerator.CCG, LinearPrices(priceMap))
                } else {
                    cca = CCAuction(domain, OutcomeRuleGenerator.CCG, config.useProposedReservePrices)
                }
                cca.setPriceUpdater(SimpleRelativePriceUpdate().withPriceUpdate(BigDecimal.valueOf(config.ccaConfig.priceUpdate)))
                cca.addSupplementaryRound(ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(config.ccaConfig.supplementaryBids))
                return cca
            }
            PVM_VCG -> PVMAuction(domain, OutcomeRuleGenerator.VCG_XOR, config.pvmConfig.initialRoundBids)
            PVM_CCG -> PVMAuction(domain, OutcomeRuleGenerator.CCG, config.pvmConfig.initialRoundBids)
        }
        auction.maxBids = config.maxBids
        auction.setDemandQueryTimeLimit(config.demandQueryTimeLimit)
        return auction
    }
}
