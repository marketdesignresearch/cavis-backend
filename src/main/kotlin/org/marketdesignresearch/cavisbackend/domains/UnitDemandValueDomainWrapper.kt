package org.marketdesignresearch.cavisbackend.domains

import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.marketdesignresearch.mechlib.core.SimpleUnitDemandDomain
import org.marketdesignresearch.mechlib.core.bidder.UnitDemandBidder

/**
 * The strict definition of a unit demand value is the following:
 * If one or more items are won, I have value X. If no item is won, I have value 0.
 * This is sub-additive, which is why we cannot use an OR domain.
 */
data class UnitDemandValueDomainWrapper(
        val bidders: List<PerItemBidder> = listOf(PerItemBidder("1"), PerItemBidder("2"), PerItemBidder("3")),
        val goods: List<SimpleGood> = listOf(SimpleGood("A"), SimpleGood("B"))
): DomainWrapper {

    init {
        if (goods.any { it.quantity != 1 } ) {
            throw RuntimeException("The current implementation of the Unit Demand domain works only for " +
                    "single-availability goods. It may not make sense to have this domain with different generic goods " +
                    "anyway. If you're modelling identical goods, declare them as separate goods.")
        }
    }

    override fun toDomain(): SimpleUnitDemandDomain {
        val unitDemandBidders = arrayListOf<UnitDemandBidder>()
        bidders.forEach { bidder ->
            val distribution = UniformIntegerDistribution(bidder.min, bidder.max)
            val value = distribution.sample().toBigDecimal()
            unitDemandBidders.add(UnitDemandBidder(bidder.name, value, goods))
        }
        return SimpleUnitDemandDomain(unitDemandBidders, goods)
    }

    override fun getName() = "Unit Demand Value Domain"


}