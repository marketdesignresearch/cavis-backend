package org.marketdesignresearch.cavisbackend.domains

import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.marketdesignresearch.mechlib.core.Bundle
import org.marketdesignresearch.mechlib.core.BundleEntry
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.marketdesignresearch.mechlib.core.SimpleORDomain
import org.marketdesignresearch.mechlib.core.bidder.AdditiveValueBidder
import org.marketdesignresearch.mechlib.core.bidder.valuefunction.BundleValue
import org.marketdesignresearch.mechlib.core.bidder.valuefunction.ORValueFunction
import java.math.BigDecimal

/**
 * One straight-forward implementation for an additive value domain is to use OR-values on the individual goods.
 */
data class AdditiveValueDomainWrapper(
        val bidders: List<PerItemBidder> = listOf(PerItemBidder("1"), PerItemBidder("2"), PerItemBidder("3")),
        val goods: List<SimpleGood> = listOf(SimpleGood("A"), SimpleGood("B"))
) : DomainWrapper {

    override fun toDomain(seed: Long): SimpleORDomain {
        val additiveBidders = arrayListOf<AdditiveValueBidder>()
        var count = 0
        bidders.forEach { bidder ->
            val distribution = UniformIntegerDistribution(bidder.min, bidder.max)
            distribution.reseedRandomGenerator(seed + count++)
            val bundleValues = hashSetOf<BundleValue>()
            goods.forEach {
                val amount = distribution.sample().toLong()
                for (i in 1..it.quantity) {
                    val bundle = Bundle(hashSetOf(BundleEntry(it, i)))
                    bundleValues.add(BundleValue(BigDecimal.valueOf(amount * i), bundle))
                }
            }
            additiveBidders.add(AdditiveValueBidder(bidder.name, ORValueFunction(bundleValues)))
        }
        return SimpleORDomain(additiveBidders, goods)
    }

    override fun getName() = "Additive Value Domain"
}