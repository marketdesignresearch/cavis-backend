package org.marketdesignresearch.cavisbackend.domains

import org.marketdesignresearch.cavisbackend.server.AuctionConfiguration
import org.marketdesignresearch.mechlib.auction.Auction
import org.marketdesignresearch.mechlib.auction.pvm.PVMAuction
import org.marketdesignresearch.mechlib.domain.Domain
import org.marketdesignresearch.mechlib.mechanisms.MechanismType
import org.marketdesignresearch.mechlib.auction.cca.CCAuction
import org.marketdesignresearch.mechlib.auction.cca.bidcollection.supplementaryround.ProfitMaximizingSupplementaryRound
import org.marketdesignresearch.mechlib.auction.cca.priceupdate.SimpleRelativePriceUpdate
import org.marketdesignresearch.mechlib.auction.sequential.SequentialAuction
import org.marketdesignresearch.mechlib.domain.Good
import org.marketdesignresearch.mechlib.domain.price.LinearPrices
import org.marketdesignresearch.mechlib.domain.price.Price
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
            SINGLE_ITEM_FIRST_PRICE, SIMULTANEOUS_FIRST_PRICE -> Auction(domain, MechanismType.FIRST_PRICE)
            SINGLE_ITEM_SECOND_PRICE, SIMULTANEOUS_SECOND_PRICE -> Auction(domain, MechanismType.SECOND_PRICE)
            SEQUENTIAL_FIRST_PRICE -> SequentialAuction(domain, MechanismType.FIRST_PRICE)
            SEQUENTIAL_SECOND_PRICE -> SequentialAuction(domain, MechanismType.SECOND_PRICE)
            VCG_XOR -> Auction(domain, MechanismType.VCG_XOR)
            VCG_OR -> Auction(domain, MechanismType.VCG_OR)
            CCA_VCG -> {
                val cca: CCAuction
                if (config.reservePrices.isNotEmpty()) {
                    val priceMap = hashMapOf<Good, Price>()
                    config.reservePrices.forEach { priceMap[domain.getGood(it.key)] = Price.of(it.value) }
                    cca = CCAuction(domain, MechanismType.VCG_XOR, LinearPrices(priceMap))
                } else {
                    cca = CCAuction(domain, MechanismType.VCG_XOR, config.useProposedReservePrices)
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
                    cca = CCAuction(domain, MechanismType.CCG, LinearPrices(priceMap))
                } else {
                    cca = CCAuction(domain, MechanismType.CCG, config.useProposedReservePrices)
                }
                cca.setPriceUpdater(SimpleRelativePriceUpdate().withPriceUpdate(BigDecimal.valueOf(config.ccaConfig.priceUpdate)))
                cca.addSupplementaryRound(ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(config.ccaConfig.supplementaryBids))
                return cca
            }
            PVM_VCG -> PVMAuction(domain, MechanismType.VCG_XOR, config.pvmConfig.initialRoundBids)
            PVM_CCG -> PVMAuction(domain, MechanismType.CCG, config.pvmConfig.initialRoundBids)
        }
        auction.maxBids = config.maxBids
        auction.setDemandQueryTimeLimit(config.demandQueryTimeLimit)
        return auction
    }
}
