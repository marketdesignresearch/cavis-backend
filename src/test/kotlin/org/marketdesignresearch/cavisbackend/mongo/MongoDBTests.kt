package org.marketdesignresearch.cavisbackend.mongo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.marketdesignresearch.cavisbackend.domains.*
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.marketdesignresearch.mechlib.outcomerules.OutcomeRule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import java.math.RoundingMode
import java.util.*
import java.util.stream.Stream

@SpringBootTest
class MongoDBTests {
    @Autowired
    lateinit var auctionWrapperDAO: AuctionWrapperDAO

    val bidders = listOf(PerItemBidder("1"), PerItemBidder("2"), PerItemBidder("3"))
    val goods = listOf(SimpleGood("A"), SimpleGood("B"))

    @TestFactory
    fun `MongoDB test for all domains and auctions`(): Stream<DynamicTest> {

        val inputDomains: List<DomainWrapper> = listOf(
                UnitDemandValueDomainWrapper(bidders, goods),
                AdditiveValueDomainWrapper(bidders, goods)
                //GSVMDomainWrapper(seed = 1234L) // TODO
                //LSVMDomainWrapper(seed = 1234L),
                //MRVMDomainWrapper(seed = 1234L)
        )

        return inputDomains.stream()
                .flatMap { domainWrapper ->
                        val domain = domainWrapper.toDomain()
                        AuctionFactory.values().asList().stream().map { auctionFactory ->
                            DynamicTest.dynamicTest("Testing DB storing & retrieving in ${domainWrapper.getName()}, using $auctionFactory") {
                            val auction = auctionFactory.getAuction(domain)
                            val auctionWrapper = AuctionWrapper(UUID.randomUUID(), auction, auctionFactory)
                            auctionWrapperDAO.save(auctionWrapper)
                            val retrieved = auctionWrapperDAO.findByIdOrNull(auctionWrapper.id) ?: fail("Could not find object in DB.")
                            assertThat(auctionWrapper).isEqualTo(retrieved)
                            while (!auction.finished()) {
                                auction.advanceRound()
                            }
                            auctionWrapperDAO.save(auctionWrapper)
                            val retrievedFinishedAuctionWrapper = auctionWrapperDAO.findByIdOrNull(auctionWrapper.id) ?: fail("Could not find object in DB.")
                            assertThat(auctionWrapper).isEqualTo(retrievedFinishedAuctionWrapper)
                        }
                    }
                }
    }

}