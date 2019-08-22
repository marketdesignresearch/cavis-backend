package org.marketdesignresearch.cavisbackend.domains

import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.marketdesignresearch.mechlib.core.Bundle
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.marketdesignresearch.mechlib.core.SimpleXORDomain
import org.marketdesignresearch.mechlib.core.bidder.XORBidder
import org.marketdesignresearch.mechlib.core.bidder.valuefunction.BundleValue
import org.marketdesignresearch.mechlib.core.bidder.valuefunction.XORValueFunction
import java.math.BigDecimal
import kotlin.math.max

/**
 * The Local-Local-Global (LLG) domain is a popular example to show VCG prices that are not in the core.
 * In the interesting case, two local bidders (only interested in one good each) win the goods, but pay less
 * than the global bidder (interested only in the two goods as a package) would have been willing to pay.
 */
data class LLGDomainWrapper(
        val interestingCase: Boolean = true,
        val maxLocalValue: Int = 100
) : DomainWrapper {

    override fun toDomain(): SimpleXORDomain {
        val goodA = SimpleGood("A")
        val goodB = SimpleGood("B")
        val valueL1 = UniformIntegerDistribution(1, maxLocalValue).sample()
        val valueL2 = UniformIntegerDistribution(1, maxLocalValue).sample()
        val valueG = when {
            interestingCase -> UniformIntegerDistribution(max(valueL1, valueL2), valueL1 + valueL2).sample()
            else -> UniformIntegerDistribution(valueL1 + valueL2 + 1, 2 * (valueL1 + valueL2)).sample()
        }
        val bidderL1 = XORBidder("Local Bidder 1", XORValueFunction(setOf(BundleValue(BigDecimal.valueOf(valueL1.toLong()), Bundle.of(goodA)))))
        val bidderL2 = XORBidder("Local Bidder 2", XORValueFunction(setOf(BundleValue(BigDecimal.valueOf(valueL2.toLong()), Bundle.of(goodB)))))
        val bidderG = XORBidder("Global Bidder", XORValueFunction(setOf(BundleValue(BigDecimal.valueOf(valueG.toLong()), Bundle.of(goodA, goodB)))))
        return SimpleXORDomain(listOf(bidderL1, bidderL2, bidderG), listOf(goodA, goodB))
    }

    override fun getName() = "Local-Local-Global (LLG) Domain"


}