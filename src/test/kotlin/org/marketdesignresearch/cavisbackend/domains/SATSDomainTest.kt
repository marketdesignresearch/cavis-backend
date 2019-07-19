package org.marketdesignresearch.cavisbackend.domains

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.marketdesignresearch.mechlib.domain.Bundle
import org.marketdesignresearch.mechlib.domain.Domain
import org.marketdesignresearch.mechlib.domain.Good
import org.marketdesignresearch.mechlib.domain.SimpleGood
import org.marketdesignresearch.mechlib.domain.bidder.Bidder
import org.marketdesignresearch.mechlib.domain.price.LinearPrices
import org.marketdesignresearch.mechlib.domain.price.Price
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.stream.Stream

@SpringBootTest
class SATSDomainTest {

    @TestFactory
    fun `SATS domain test factory`(): Stream<DynamicTest> {
        val inputDomains: List<DomainWrapper> = listOf(
                GSVMDomainWrapper(seed = 1234L),
                LSVMDomainWrapper(seed = 1234L),
                MRVMDomainWrapper(seed = 1234L)
        )

        val totalValues: List<String> = listOf(
                "427.2671",
                "550.4388",
                "11658633670.1631"
        )

        val winners: List<List<Int>> = listOf(
                listOf(2, 3, 1, 5),
                listOf(2, 3, 1, 5),
                listOf(3, 8, 7, 4, 5, 9, 6)
        )


        return inputDomains.stream()
                .map { domainWrapper ->
                    DynamicTest.dynamicTest("Testing ${domainWrapper.getName()}") {
                        val id = inputDomains.indexOf(domainWrapper)
                        val domain = domainWrapper.toDomain()
                        assertThat(domain.efficientAllocation.totalAllocationValue.setScale(4, RoundingMode.DOWN)).isEqualByComparingTo(totalValues[id])
                        val winnerArray = winners[id].map { domain.bidders[it] }.toTypedArray()
                        assertThat(domain.efficientAllocation.winners).containsExactlyInAnyOrder(*winnerArray)
                    }
                }

    }

}