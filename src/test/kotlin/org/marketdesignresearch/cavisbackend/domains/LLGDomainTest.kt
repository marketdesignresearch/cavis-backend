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
class LLGDomainTest {

    @Test
    fun `Should create valid interesting LLG Domain`() {
        val domain = LLGDomainWrapper().toDomain() as SimpleXORDomain

        val goodA = domain.goods[0]
        val goodB = domain.goods[1]
        val bidder1 = domain.bidders[0]
        val bidder2 = domain.bidders[1]
        val bidder3 = domain.bidders[2]
        val A = Bundle.of(goodA)
        val B = Bundle.of(goodB)
        val AB = Bundle.of(goodA, goodB)

        assertThat(goodA.name).isEqualTo("A")
        assertThat(goodB.name).isEqualTo("B")
        assertThat(bidder1.name).isEqualTo("Local Bidder 1")
        assertThat(bidder2.name).isEqualTo("Local Bidder 2")
        assertThat(bidder3.name).isEqualTo("Global Bidder")

        assertThat(domain.efficientAllocation.winners).containsExactlyInAnyOrder(bidder1, bidder2)

        assertThat(bidder1.getValue(A).add(bidder2.getValue(B)).toDouble()).isGreaterThanOrEqualTo(bidder3.getValue(AB).toDouble())

        // VCG
        val vcgRule = XORVCGRule(Bids.fromXORBidders(domain.bidders))
        assertThat(vcgRule.allocation.winners).isEqualTo(domain.efficientAllocation.winners)
        assertThat(vcgRule.payment.totalPayments.toDouble()).isLessThanOrEqualTo(bidder3.getValue(AB).toDouble())

        // CCG
        val ccgRule = OutcomeRuleGenerator.CCG.getOutcomeRule(Bids.fromXORBidders(domain.bidders))
        assertThat(ccgRule.allocation.winners).isEqualTo(vcgRule.allocation.winners)
        assertThat(ccgRule.payment.totalPayments.toDouble()).isGreaterThan(vcgRule.payment.totalPayments.toDouble())
        assertThat(ccgRule.payment.totalPayments.toDouble()).isGreaterThanOrEqualTo(bidder3.getValue(AB).toDouble())
    }

    @Test
    fun `Should create valid uninteresting LLG Domain`() {
        val domain = LLGDomainWrapper(interestingCase = false).toDomain() as SimpleXORDomain
        val A = Bundle.of(domain.goods[0])
        val B = Bundle.of(domain.goods[1])
        val AB = Bundle.of(domain.goods[0], domain.goods[1])

        // VCG
        val vcgRule = XORVCGRule(Bids.fromXORBidders(domain.bidders))
        assertThat(vcgRule.payment.totalPayments).isEqualByComparingTo(domain.bidders[0].getValue(A).add(domain.bidders[1].getValue(B)))

        // CCG
        val ccgRule = OutcomeRuleGenerator.CCG.getOutcomeRule(Bids.fromXORBidders(domain.bidders))
        assertThat(ccgRule.allocation.winners).isEqualTo(vcgRule.allocation.winners)
        assertThat(ccgRule.payment.totalPayments).isEqualByComparingTo(vcgRule.payment.totalPayments)

    }

    @Test
    fun `Should sample same values with same seed`() {
        val domainWrapper = LLGDomainWrapper()

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