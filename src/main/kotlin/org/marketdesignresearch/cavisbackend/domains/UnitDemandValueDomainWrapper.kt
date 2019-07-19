package org.marketdesignresearch.cavisbackend.domains

import com.marcinmoskala.math.powerset
import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.marketdesignresearch.mechlib.domain.*
import org.marketdesignresearch.mechlib.domain.bidder.XORBidder
import org.marketdesignresearch.mechlib.domain.bidder.value.BundleValue
import org.marketdesignresearch.mechlib.domain.bidder.value.XORValue
import java.math.BigDecimal

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
        if (goods.any { it.available() != 1 } ) {
            throw RuntimeException("The current implementation of the Unit Demand domain works only for " +
                    "single-availability goods. It may not make sense to have this domain with different generic goods " +
                    "anyway. If you're modelling identical goods, declare them as separate goods.")
        }
    }

    override fun toDomain(): SimpleXORDomain {
        val xorBidders = arrayListOf<XORBidder>()
        bidders.forEach { bidder ->
            val distribution = UniformIntegerDistribution(bidder.min, bidder.max)
            val amount = distribution.sample().toLong()
            val value = XORValue()
            for (combination in goods.powerset()) {
                val bundleEntries = combination.map { g -> BundleEntry(g, 1) }.toSet()
                val bundle = Bundle(bundleEntries)
                val v = if (combination.isNotEmpty()) BigDecimal.valueOf(amount) else BigDecimal.ZERO
                value.addBundleValue(BundleValue(v, bundle))
            }
            xorBidders.add(XORBidder(bidder.name, value))
        }
        return SimpleXORDomain(xorBidders, goods)
    }

    override fun getName() = "Unit Demand Value Domain"


}