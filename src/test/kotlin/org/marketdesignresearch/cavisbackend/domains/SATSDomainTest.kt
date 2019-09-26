package org.marketdesignresearch.cavisbackend.domains

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.math.RoundingMode
import java.util.stream.Stream

@ExtendWith(SpringExtension::class)
class SATSDomainTest {

    @TestFactory
    fun `SATS domain test factory`(): Stream<DynamicTest> {
        val inputDomains: List<DomainWrapper> = listOf(
                GSVMDomainWrapper(),
                LSVMDomainWrapper(),
                MRVMDomainWrapper()
        )

        val totalValues: List<String> = listOf(
                "405.1789",
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
                        val domain = domainWrapper.toDomain(1234L)
                        assertThat(domain.efficientAllocation.totalAllocationValue.setScale(4, RoundingMode.DOWN)).isEqualByComparingTo(totalValues[id])
                        val winnerArray = winners[id].map { domain.bidders[it] }.toTypedArray()
                        assertThat(domain.efficientAllocation.winners).containsExactlyInAnyOrder(*winnerArray)
                    }
                }

    }

}