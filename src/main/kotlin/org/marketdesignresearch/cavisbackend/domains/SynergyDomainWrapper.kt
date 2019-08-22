package org.marketdesignresearch.cavisbackend.domains

import com.marcinmoskala.math.powerset
import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.marketdesignresearch.mechlib.core.*
import org.marketdesignresearch.mechlib.core.bidder.XORBidder
import org.marketdesignresearch.mechlib.core.bidder.valuefunction.BundleValue
import org.marketdesignresearch.mechlib.core.bidder.valuefunction.XORValueFunction
import java.math.BigDecimal
import kotlin.math.max

/**
 * This wrapper creates a domain where there exists a simple (positive or negative) synergy among the goods.
 * The value function corrects the additive value by a factor that is dependent on
 *   a) The synergy (negative if the goods are substitutes, positive if the goods are complements)
 *   b) the bundle size
 */
data class SynergyDomainWrapper(
        val bidders: List<PerItemBidder> = listOf(PerItemBidder("1"), PerItemBidder("2"), PerItemBidder("3")),
        val goods: List<SimpleGood> = listOf(SimpleGood("A"), SimpleGood("B")),
        val synergy: Double = 0.2
) : DomainWrapper {

    init {
        if (goods.any { it.quantity != 1 } ) {
            throw RuntimeException("The current implementation of the Synergy Domain works only for " +
                    "single-availability goods. It may not make sense to have this domain with different generic goods " +
                    "anyway. If you're modelling identical goods, declare them as separate goods.")
        }
    }

    override fun toDomain(): SimpleXORDomain {
        val xorBidders = arrayListOf<XORBidder>()
        bidders.forEach { bidder ->
            val distribution = UniformIntegerDistribution(bidder.min, bidder.max)
            val values = hashMapOf<SimpleGood, Int>()
            goods.forEach { values[it] = distribution.sample() }
            val bundleValues = hashSetOf<BundleValue>()
            for (combination in goods.powerset()) {
                if (combination.isEmpty()) continue
                var value = combination.map { values[it] ?: 0 }.reduce { a, b -> a + b}.toDouble()
                if (combination.size > 1) value += value * synergy * (combination.size - 1)
                if (value > 0) {
                    val bundleEntries = combination.map { g -> BundleEntry(g, 1) }.toSet()
                    val bundle = Bundle(bundleEntries)
                    bundleValues.add(BundleValue(BigDecimal.valueOf(value), bundle))
                }
            }
            xorBidders.add(XORBidder(bidder.name, XORValueFunction(bundleValues)))
        }
        return SimpleXORDomain(xorBidders, goods)
    }

    override fun getName() = "Simple Synergy Domain"


}