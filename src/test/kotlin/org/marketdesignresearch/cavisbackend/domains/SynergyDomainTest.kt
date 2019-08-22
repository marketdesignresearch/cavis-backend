package org.marketdesignresearch.cavisbackend.domains

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.marketdesignresearch.mechlib.core.Bundle
import org.marketdesignresearch.mechlib.core.Good
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.marketdesignresearch.mechlib.core.SimpleXORDomain
import org.marketdesignresearch.mechlib.core.bid.Bids
import org.marketdesignresearch.mechlib.core.bidder.Bidder
import org.marketdesignresearch.mechlib.core.price.LinearPrices
import org.marketdesignresearch.mechlib.core.price.Price
import org.marketdesignresearch.mechlib.core.price.Prices
import org.marketdesignresearch.mechlib.outcomerules.OutcomeRuleGenerator
import org.marketdesignresearch.mechlib.outcomerules.ccg.CCGFactory
import org.marketdesignresearch.mechlib.outcomerules.ccg.CCGOutcomeRule
import org.marketdesignresearch.mechlib.outcomerules.vcg.VCGRule
import org.marketdesignresearch.mechlib.outcomerules.vcg.XORVCGRule
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal

@ExtendWith(SpringExtension::class)
class SynergyDomainTest {

    private val goodA = SimpleGood("A")
    private val goodB = SimpleGood("B")
    private val goodC = SimpleGood("C")
    private val A = Bundle.of(goodA)
    private val B = Bundle.of(goodB)
    private val C = Bundle.of(goodC)
    private val AB = Bundle.of(goodA, goodB)
    private val AC = Bundle.of(goodA, goodC)
    private val BC = Bundle.of(goodB, goodC)
    private val ABC = Bundle.of(goodA, goodB, goodC)

    @Test
    fun `Should create valid super-additive Synergy Domain`() {
        val domain = SynergyDomainWrapper(
                listOf(PerItemBidder("1", 10, 10),
                        PerItemBidder("2", 13, 13),
                        PerItemBidder("3", 16, 16)),
                listOf(goodA, goodB, goodC),
                0.4)
                .toDomain()

        val bidder1 = domain.getBidder("1")
        val bidder2 = domain.getBidder("2")
        val bidder3 = domain.getBidder("3")

        assertThat(domain.bidders.map { it.name }).containsExactly("1", "2", "3")
        assertThat(domain.goods).isEqualTo(listOf(goodA, goodB, goodC))

        assertThat(domain.efficientAllocation.winners).containsOnly(bidder3)
        assertThat(domain.efficientAllocation.totalAllocationValue).isEqualByComparingTo("86.4")
        assertThat(domain.efficientAllocation.allocationOf(bidder1).bundle).isEqualTo(Bundle.EMPTY)
        assertThat(domain.efficientAllocation.allocationOf(bidder2).bundle).isEqualTo(Bundle.EMPTY)
        assertThat(domain.efficientAllocation.allocationOf(bidder3).bundle).isEqualTo(ABC)

        checkBidder(bidder1, 10, 0.4)
        checkBidder(bidder2, 13, 0.4)
        checkBidder(bidder3, 16, 0.4)
    }

    @Test
    fun `Should create valid sub-additive Synergy Domain`() {
        val domain = SynergyDomainWrapper(
                listOf(PerItemBidder("1", 10, 10),
                        PerItemBidder("2", 13, 13),
                        PerItemBidder("3", 16, 16)),
                listOf(goodA, goodB, goodC),
                -0.1)
                .toDomain()

        val bidder1 = domain.getBidder("1")
        val bidder2 = domain.getBidder("2")
        val bidder3 = domain.getBidder("3")

        assertThat(domain.bidders.map { it.name }).containsExactly("1", "2", "3")
        assertThat(domain.goods).isEqualTo(listOf(goodA, goodB, goodC))

        assertThat(domain.efficientAllocation.winners).containsExactlyInAnyOrder(bidder2, bidder3)
        assertThat(domain.efficientAllocation.totalAllocationValue).isEqualByComparingTo("41.8")
        assertThat(domain.efficientAllocation.allocationOf(bidder1).bundle).isEqualTo(Bundle.EMPTY)

        checkBidder(bidder1, 10, -0.1)
        checkBidder(bidder2, 13, -0.1)
        checkBidder(bidder3, 16, -0.1)
    }

    private fun checkBidder(bidder: Bidder, value: Long, synergy: Double) {
        assertThat(bidder.getValue(A)).isEqualByComparingTo(BigDecimal.valueOf(value))
        assertThat(bidder.getValue(B)).isEqualByComparingTo(BigDecimal.valueOf(value))
        assertThat(bidder.getValue(C)).isEqualByComparingTo(BigDecimal.valueOf(value))
        assertThat(bidder.getValue(AB)).isEqualByComparingTo(BigDecimal.valueOf(value * 2 + (value * 2 * synergy)))
        assertThat(bidder.getValue(AC)).isEqualByComparingTo(BigDecimal.valueOf(value * 2 + (value * 2 * synergy)))
        assertThat(bidder.getValue(BC)).isEqualByComparingTo(BigDecimal.valueOf(value * 2 + (value * 2 * synergy)))
        assertThat(bidder.getValue(ABC)).isEqualByComparingTo(BigDecimal.valueOf(value * 3 + (value * 3 * synergy * 2)))
    }
}