package org.marketdesignresearch.cavisbackend.domains

import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.marketdesignresearch.mechlib.domain.*
import org.marketdesignresearch.mechlib.domain.bidder.ORBidder
import org.marketdesignresearch.mechlib.domain.bidder.value.BundleValue
import org.marketdesignresearch.mechlib.domain.bidder.value.ORValue
import java.math.BigDecimal

/**
 * One straight-forward implementation for an additive value domain is to use OR-values on the individual goods.
 */
data class AdditiveValueDomainWrapper(
        val bidders: List<PerItemBidder> = listOf(PerItemBidder("1"), PerItemBidder("2"), PerItemBidder("3")),
        val goods: List<SimpleGood> = listOf(SimpleGood("A"), SimpleGood("B"))
) : DomainWrapper {

    override fun toDomain(): SimpleORDomain {
        val orBidders = arrayListOf<ORBidder>()
        bidders.forEach { bidder ->
            val distribution = UniformIntegerDistribution(bidder.min, bidder.max)
            val value = ORValue()
            goods.forEach {
                val amount = distribution.sample().toLong()
                for (i in 1..it.available()) {
                    val bundle = Bundle(hashSetOf(BundleEntry(it, i)))
                    value.addBundleValue(BundleValue(BigDecimal.valueOf(amount * i), bundle))
                }
            }
            orBidders.add(ORBidder(bidder.name, value))
        }
        return SimpleORDomain(orBidders, goods)
    }

    override fun getName() = "Additive Value Domain"
}