package org.marketdesignresearch.cavisbackend.mongo

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.marketdesignresearch.cavisbackend.domains.*
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.marketdesignresearch.mechlib.outcomerules.OutcomeRule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.RoundingMode
import java.util.*
import java.util.stream.Stream

@SpringBootTest
internal class MongoDBTests {
    @Autowired
    private lateinit var auctionWrapperDAO: AuctionWrapperDAO

    @TestFactory
    fun `MongoDB test for all domains and auctions`(): Stream<DynamicTest> {

        val inputDomains: List<DomainWrapper> = listOf(
                UnitDemandValueDomainWrapper(),
                AdditiveValueDomainWrapper(),
                SynergyDomainWrapper(),
                LLGDomainWrapper()
                //GSVMDomainWrapper(seed = 1234L) // TODO
                //LSVMDomainWrapper(seed = 1234L),
                //MRVMDomainWrapper(seed = 1234L)
        )

        return inputDomains.stream()
                .flatMap { domainWrapper ->
                    val seed = System.currentTimeMillis()
                    val domain = domainWrapper.toDomain(seed)
                    AuctionFactory.values().asList().stream().map { auctionFactory ->
                        DynamicTest.dynamicTest("Testing DB storing & retrieving in ${domainWrapper.getName()}, using $auctionFactory") {
                            val auction = auctionFactory.getAuction(domain)
                            val auctionWrapper = AuctionWrapper(UUID.randomUUID(), auction, auctionFactory, seed)
                            auctionWrapperDAO.save(auctionWrapper)
                            val retrieved = auctionWrapperDAO.findByIdOrNull(auctionWrapper.id)
                                    ?: fail("Could not find object in DB.")
                            assertThat(auctionWrapper).isEqualTo(retrieved)
                            while (!auction.finished()) {
                                auction.advanceRound()
                            }
                            auctionWrapperDAO.save(auctionWrapper)
                            val retrievedFinishedAuctionWrapper = auctionWrapperDAO.findByIdOrNull(auctionWrapper.id)
                                    ?: fail("Could not find object in DB.")
                            assertThat(auctionWrapper).isEqualTo(retrievedFinishedAuctionWrapper)
                        }
                    }
                }
    }

}