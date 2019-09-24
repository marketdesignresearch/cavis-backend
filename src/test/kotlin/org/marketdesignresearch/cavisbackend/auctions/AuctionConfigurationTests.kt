package org.marketdesignresearch.cavisbackend.auctions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.marketdesignresearch.cavisbackend.api.AuctionConfiguration
import org.marketdesignresearch.cavisbackend.api.PaymentRule
import org.marketdesignresearch.cavisbackend.domains.AdditiveValueDomainWrapper
import org.marketdesignresearch.cavisbackend.domains.AuctionFactory
import org.marketdesignresearch.cavisbackend.domains.PerItemBidder
import org.marketdesignresearch.mechlib.core.Bundle
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCAuction
import org.marketdesignresearch.mechlib.outcomerules.OutcomeRuleGenerator
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest
class AuctionConfigurationTests {
    @Test
    fun `Should create a custom CCA auction`() {
        val bidders = listOf(PerItemBidder("1", min = 5, max = 5), PerItemBidder("2", min = 8, max = 8))
        val itemA = SimpleGood("A")
        val itemB = SimpleGood("B")
        val goods = listOf(itemA, itemB)
        val domain = AdditiveValueDomainWrapper(bidders, goods).toDomain()
        val config = AuctionConfiguration()
        config.maxBids = 20
        config.demandQueryTimeLimit = 30.0
        config.useProposedReservePrices = false
        config.ccaConfig.maxRounds = 50
        config.ccaConfig.priceUpdate = 0.25
        config.ccaConfig.initialPriceUpdateIfPriceEqualsZero = 1.5
        config.ccaConfig.supplementaryBids = 15
        config.ccaConfig.paymentRule = PaymentRule.CCG

        val auction = AuctionFactory.CCA.getAuction(domain, config) as CCAuction

        // Check bidders
        assertThat(auction.domain.bidders).hasSize(2)
        assertThat(auction.domain.bidders[0].name).isEqualTo("1")
        assertThat(auction.domain.bidders[0].getValue(Bundle.of(itemA))).isEqualByComparingTo("5")
        assertThat(auction.domain.bidders[0].getValue(Bundle.of(itemA))).isEqualByComparingTo("5")
        assertThat(auction.domain.bidders[0].getValue(Bundle.of(itemA, itemB))).isEqualByComparingTo("10")
        assertThat(auction.domain.bidders[1].name).isEqualTo("2")
        assertThat(auction.domain.bidders[1].getValue(Bundle.of(itemA))).isEqualByComparingTo("8")
        assertThat(auction.domain.bidders[1].getValue(Bundle.of(itemA))).isEqualByComparingTo("8")
        assertThat(auction.domain.bidders[1].getValue(Bundle.of(itemA, itemB))).isEqualByComparingTo("16")

        // Check items
        assertThat(auction.domain.goods).hasSize(2)
        assertThat(auction.domain.goods[0].uuid).isEqualTo(itemA.uuid)
        assertThat(auction.domain.goods[0]).isEqualTo(itemA)
        assertThat(auction.domain.goods[1].uuid).isEqualTo(itemB.uuid)
        assertThat(auction.domain.goods[1]).isEqualTo(itemB)

        // Check auction
        assertThat(auction.outcomeRuleGenerator).isEqualTo(OutcomeRuleGenerator.CCG)
        //assertThat(auction.maxBids).isEqualTo(20) // FIXME -> is still the default (100)
        assertThat(auction.maxRounds).isEqualTo(50)
        //assertThat(auction.demandQueryTimeLimit).isEqualTo(20) // FIXME -> is still the default (-1)
        assertThat(auction.currentPrices.getPrice(Bundle.of(itemA)).amount).isEqualTo(BigDecimal.ZERO)
        assertThat(auction.currentPrices.getPrice(Bundle.of(itemB)).amount).isEqualTo(BigDecimal.ZERO)
        assertThat(auction.currentPrices.getPrice(Bundle.of(itemA, itemB)).amount).isEqualTo(BigDecimal.ZERO)
        assertThat(auction.supplementaryRounds).hasSize(1)
        assertThat(auction.supplementaryRounds[0].numberOfSupplementaryBids).isEqualTo(15)

        // Check price update config
        auction.advanceRound()
        assertThat(auction.currentPrices.getPrice(Bundle.of(itemA)).amount).isEqualByComparingTo("1.5")
        assertThat(auction.currentPrices.getPrice(Bundle.of(itemB)).amount).isEqualByComparingTo("1.5")
        assertThat(auction.currentPrices.getPrice(Bundle.of(itemA, itemB)).amount).isEqualByComparingTo("3")

        auction.advanceRound()
        assertThat(auction.currentPrices.getPrice(Bundle.of(itemA)).amount).isEqualByComparingTo("1.875")
        assertThat(auction.currentPrices.getPrice(Bundle.of(itemB)).amount).isEqualByComparingTo("1.875")
        assertThat(auction.currentPrices.getPrice(Bundle.of(itemA, itemB)).amount).isEqualByComparingTo("3.75")

    }
}