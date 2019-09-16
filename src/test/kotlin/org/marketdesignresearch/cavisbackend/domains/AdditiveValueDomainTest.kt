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
import org.marketdesignresearch.mechlib.core.price.Prices
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.BigDecimal

@ExtendWith(SpringExtension::class)
class AdditiveValueDomainTest {

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
    fun `Should create valid Additive Value Domain`() {
        val domain = AdditiveValueDomainWrapper(
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

        assertThat(domain.efficientAllocation.winners).containsOnly(bidder3)
        assertThat(domain.efficientAllocation.totalAllocationValue).isEqualByComparingTo("48")
        assertThat(domain.efficientAllocation.allocationOf(bidder1).bundle).isEqualTo(Bundle.EMPTY)
        assertThat(domain.efficientAllocation.allocationOf(bidder2).bundle).isEqualTo(Bundle.EMPTY)
        assertThat(domain.efficientAllocation.allocationOf(bidder3).bundle).isEqualTo(ABC)

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
        assertThat(bidder.getValue(AB)).isEqualByComparingTo(BigDecimal.valueOf(value * 2))
        assertThat(bidder.getValue(AC)).isEqualByComparingTo(BigDecimal.valueOf(value * 2))
        assertThat(bidder.getValue(BC)).isEqualByComparingTo(BigDecimal.valueOf(value * 2))
        assertThat(bidder.getValue(ABC)).isEqualByComparingTo(BigDecimal.valueOf(value * 3))
        var prices = Prices.NONE
        assertThat(bidder.getBestBundle(prices)).isEqualTo(ABC)
        prices = LinearPrices(mapOf(goodA as Good to Price.of(value - 1.0), goodB as Good to Price.of(value - 1.0), goodC as Good to Price.of(value + 1.0)))
        assertThat(bidder.getBestBundle(prices)).isEqualTo(AB)
        prices = LinearPrices(mapOf(goodA as Good to Price.of(value + 1.0), goodB as Good to Price.of(value - 1.0), goodC as Good to Price.of(value - 1.0)))
        assertThat(bidder.getBestBundle(prices)).isEqualTo(BC)
        prices = LinearPrices(mapOf(goodA as Good to Price.of(value - 1.0), goodB as Good to Price.of(value + 1.0), goodC as Good to Price.of(value - 1.0)))
        assertThat(bidder.getBestBundle(prices)).isEqualTo(AC)
        prices = LinearPrices(mapOf(goodA as Good to Price.of(value - 1.0), goodB as Good to Price.of(value + 1.0), goodC as Good to Price.of(value + 1.0)))
        assertThat(bidder.getBestBundle(prices)).isEqualTo(A)
        prices = LinearPrices(mapOf(goodA as Good to Price.of(value + 1.0), goodB as Good to Price.of(value - 1.0), goodC as Good to Price.of(value + 1.0)))
        assertThat(bidder.getBestBundle(prices)).isEqualTo(B)
        prices = LinearPrices(mapOf(goodA as Good to Price.of(value + 1.0), goodB as Good to Price.of(value + 1.0), goodC as Good to Price.of(value - 1.0)))
        assertThat(bidder.getBestBundle(prices)).isEqualTo(C)
        prices = LinearPrices(mapOf(goodA as Good to Price.of(value + 1.0), goodB as Good to Price.of(value + 1.0), goodC as Good to Price.of(value + 1.0)))
        assertThat(bidder.getBestBundle(prices)).isEqualTo(Bundle.EMPTY)
        prices = LinearPrices(mapOf(goodA as Good to Price.of(value - 2.0), goodB as Good to Price.of(value + 1.0), goodC as Good to Price.of(value + 5.0)))
        assertThat(bidder.getBestBundles(prices, 5)).containsExactly(A, AB, Bundle.EMPTY)
        assertThat(bidder.getBestBundles(prices, 5, true)).containsExactly(A, AB, Bundle.EMPTY, B, AC)
        assertThat(bidder.getBestBundles(prices, 10, true)).containsExactly(A, AB, Bundle.EMPTY, B, AC, ABC, C, BC)
    }

    @Test
    fun `Should sample same values with same seed`() {
        val domainWrapper = AdditiveValueDomainWrapper(
                listOf(PerItemBidder("1"), PerItemBidder("2"), PerItemBidder("3")),
                listOf(goodA, goodB, goodC))

        val domain1 = domainWrapper.toDomain(54321)
        val domain2 = domainWrapper.toDomain(54321)
        val domain3 = domainWrapper.toDomain(54322)

        // TODO: Make bidder comparison easier...
        assertThat(domain1.bidders.map { it.value.bundleValues.map { bv -> bv.amount}.toSet() }.toSet() )
                .isEqualTo(domain2.bidders.map { it.value.bundleValues.map { bv -> bv.amount}.toSet() }.toSet() )
                .isNotEqualTo(domain3.bidders.map { it.value.bundleValues.map { bv -> bv.amount}.toSet() }.toSet() )
        assertThat(domain1.goods).isEqualTo(domain2.goods).isEqualTo(domain3.goods)

    }

}