package org.marketdesignresearch.cavisbackend.mongo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.marketdesignresearch.cavisbackend.domains.AuctionFactory
import org.marketdesignresearch.cavisbackend.domains.PerItemBidder
import org.marketdesignresearch.cavisbackend.domains.UnitDemandValueDomainWrapper
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.util.*

class MongoDBTests {
    @SpringBootTest
    class ConversionTests {
        @Autowired
        lateinit var auctionWrapperDAO: AuctionWrapperDAO

        @Test
        fun `Should create, store and retrieve an auction wrapper object`() {
            val bidders = listOf(PerItemBidder("1"), PerItemBidder("2"), PerItemBidder("3"))
            val goods = listOf(SimpleGood("A"), SimpleGood("B"))
            val domain = UnitDemandValueDomainWrapper(bidders, goods).toDomain()
            val auction = AuctionFactory.CCA_VCG.getAuction(domain)
            val auctionWrapper = AuctionWrapper(UUID.randomUUID(), auction, AuctionFactory.CCA_VCG)
            auctionWrapperDAO.save(auctionWrapper)
            val retrieved = auctionWrapperDAO.findByIdOrNull(auctionWrapper.id) ?: fail("Could not find object in DB.")
            assertThat(auctionWrapper).isEqualTo(retrieved)
        }
    }
}