package org.marketdesignresearch.cavisbackend.domains

import org.marketdesignresearch.mechlib.auction.Auction
import org.marketdesignresearch.mechlib.auction.pvm.PVMAuction
import org.marketdesignresearch.mechlib.domain.Domain
import org.marketdesignresearch.mechlib.mechanisms.MechanismType
import org.marketdesignresearch.mechlib.auction.cca.CCAuction
import org.marketdesignresearch.mechlib.auction.cca.bidcollection.supplementaryround.ProfitMaximizingSupplementaryRound
import org.marketdesignresearch.mechlib.auction.sequential.SequentialAuction

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

    fun getAuction(domain: Domain): Auction {
        return when (this) {
            SINGLE_ITEM_FIRST_PRICE, SIMULTANEOUS_FIRST_PRICE -> Auction(domain, MechanismType.FIRST_PRICE)
            SINGLE_ITEM_SECOND_PRICE, SIMULTANEOUS_SECOND_PRICE -> Auction(domain, MechanismType.SECOND_PRICE)
            SEQUENTIAL_FIRST_PRICE -> SequentialAuction(domain, MechanismType.FIRST_PRICE)
            SEQUENTIAL_SECOND_PRICE -> SequentialAuction(domain, MechanismType.SECOND_PRICE)
            VCG_XOR -> {
                val auction = Auction(domain, MechanismType.VCG_XOR)
                auction.maxBids = 10
                return auction
            }
            VCG_OR -> {
                val auction = Auction(domain, MechanismType.VCG_OR)
                auction.maxBids = 10
                return auction
            }
            CCA_VCG -> {
                val cca = CCAuction(domain, MechanismType.VCG_XOR, true)
                cca.addSupplementaryRound(ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(10))
                return cca
            }
            CCA_CCG -> {
                val cca = CCAuction(domain, MechanismType.CCG, true)
                cca.addSupplementaryRound(ProfitMaximizingSupplementaryRound(cca).withNumberOfSupplementaryBids(10))
                return cca
            }
            PVM_VCG -> PVMAuction(domain, MechanismType.VCG_XOR)
            PVM_CCG -> PVMAuction(domain, MechanismType.CCG)
        }
    }
}
