package org.marketdesignresearch.cavisbackend.domains

import com.marcinmoskala.math.powerset
import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.marketdesignresearch.mechlib.domain.*
import org.marketdesignresearch.mechlib.domain.bidder.ORBidder
import org.marketdesignresearch.mechlib.domain.bidder.XORBidder
import org.marketdesignresearch.mechlib.domain.bidder.value.BundleValue
import org.marketdesignresearch.mechlib.domain.bidder.value.ORValue
import org.marketdesignresearch.mechlib.domain.bidder.value.XORValue
import org.slf4j.LoggerFactory
import java.lang.UnsupportedOperationException
import java.math.BigDecimal

/**
 * The strict definition of a unit demand value is the following:
 * If one or more items are won, I have value X. If no item is won, I have value 0.
 * This is sub-additive, which is why we cannot use an OR domain.
 * The current implementation adds
 */
data class UnitDemandValueDomain(val bidders: List<PerItemBidder>, val goods: List<SimpleGood>): DomainWrapper {

    init {
        if (goods.any { it.available() != 1 } ) {
            throw RuntimeException("The current implementation of the Unit Demand domain works only for " +
                    "single-availability goods. It may not make sense to have this domain with different generic goods" +
                    "anyway. If you're modelling identical goods, declare them as separate goods")
        }
    }

    override fun toDomain(): Domain {
        val xorBidders = arrayListOf<XORBidder>()
        bidders.forEach { bidder ->
            val distribution = UniformIntegerDistribution(bidder.min, bidder.max)
            val amount = distribution.sample().toLong()
            val value = XORValue()
            for (combination in goods.powerset()) {
                val bundleEntries = combination.map { g -> BundleEntry(g, 1) }.toHashSet()
                val bundle = Bundle(bundleEntries)
                value.addBundleValue(BundleValue(BigDecimal.valueOf(amount), bundle))
            }
            xorBidders.add(XORBidder(bidder.name, value))
        }
        return SimpleXORDomain(xorBidders, goods)
    }
}