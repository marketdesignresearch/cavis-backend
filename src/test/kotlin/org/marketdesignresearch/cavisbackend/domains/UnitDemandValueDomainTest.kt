package org.marketdesignresearch.cavisbackend.domains

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.marketdesignresearch.mechlib.core.Bundle
import org.marketdesignresearch.mechlib.core.Good
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.marketdesignresearch.mechlib.core.bidder.Bidder
import org.marketdesignresearch.mechlib.core.price.LinearPrices
import org.marketdesignresearch.mechlib.core.price.Price
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal

@ExtendWith(SpringExtension::class)
class UnitDemandValueDomainTest {

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
    fun `Should create valid Unit Demand Domain`() {
        val domain = UnitDemandValueDomainWrapper(
                listOf(PerItemBidder("1", 10, 10),
                        PerItemBidder("2", 13, 13),
                        PerItemBidder("3", 16, 16)),
                listOf(goodA, goodB, goodC))
                .toDomain()

        val bidder1 = domain.getBidder("1")
        val bidder2 = domain.getBidder("2")
        val bidder3 = domain.getBidder("3")

        assertThat(domain.bidders.map { it.name }).containsExactly("1", "2", "3")
        assertThat(domain.goods).isEqualTo(listOf(goodA, goodB, goodC))

        assertThat(domain.efficientAllocation.winners).containsExactlyInAnyOrder(bidder1, bidder2, bidder3)
        assertThat(domain.efficientAllocation.totalAllocationValue).isEqualByComparingTo("39")
        assertThat(domain.efficientAllocation.allocationOf(bidder1).value).isEqualByComparingTo("10")
        assertThat(domain.efficientAllocation.allocationOf(bidder2).value).isEqualByComparingTo("13")
        assertThat(domain.efficientAllocation.allocationOf(bidder3).value).isEqualByComparingTo("16")

        // Test proposed starting prices
        val proposedStartingPrices = domain.proposeStartingPrices()
        assertThat(proposedStartingPrices.getPrice(A).amount).isEqualByComparingTo("1.3")
        assertThat(proposedStartingPrices.getPrice(B).amount).isEqualByComparingTo("1.3")
        assertThat(proposedStartingPrices.getPrice(C).amount).isEqualByComparingTo("1.3")
        assertThat(proposedStartingPrices.getPrice(AB).amount).isEqualByComparingTo("2.6")
        assertThat(proposedStartingPrices.getPrice(AC).amount).isEqualByComparingTo("2.6")
        assertThat(proposedStartingPrices.getPrice(BC).amount).isEqualByComparingTo("2.6")
        assertThat(proposedStartingPrices.getPrice(ABC).amount).isEqualByComparingTo("3.9")

        // Bidder 1
        checkBidder(bidder1, 10)
        checkBidder(bidder2, 13)
        checkBidder(bidder3, 16)

    }

    private fun checkBidder(bidder: Bidder, value: Long) {
        assertThat(bidder.getValue(A)).isEqualByComparingTo(BigDecimal.valueOf(value))
        assertThat(bidder.getValue(B)).isEqualByComparingTo(BigDecimal.valueOf(value))
        assertThat(bidder.getValue(C)).isEqualByComparingTo(BigDecimal.valueOf(value))
        assertThat(bidder.getValue(AB)).isEqualByComparingTo(BigDecimal.valueOf(value))
        assertThat(bidder.getValue(AC)).isEqualByComparingTo(BigDecimal.valueOf(value))
        assertThat(bidder.getValue(BC)).isEqualByComparingTo(BigDecimal.valueOf(value))
        assertThat(bidder.getValue(ABC)).isEqualByComparingTo(BigDecimal.valueOf(value))

        val prices = LinearPrices(mapOf(goodA as Good to Price.of(value / 2.5), goodB as Good to Price.of(value / 2.5), goodC as Good to Price.of(value / 2.5)))
        assertThat(bidder.getBestBundles(prices, 5)).hasSize(5)
        assertThat(bidder.getBestBundles(prices, 10)).containsExactlyInAnyOrder(A, B, C, AB, AC, BC, Bundle.EMPTY)
        assertThat(bidder.getBestBundles(prices, 10, true)).containsExactlyInAnyOrder(A, B, C, AB, AC, BC, Bundle.EMPTY, ABC)
    }

}