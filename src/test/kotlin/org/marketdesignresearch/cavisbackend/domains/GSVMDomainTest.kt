package org.marketdesignresearch.cavisbackend.domains

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.marketdesignresearch.mechlib.domain.Bundle
import org.marketdesignresearch.mechlib.domain.Good
import org.marketdesignresearch.mechlib.domain.SimpleGood
import org.marketdesignresearch.mechlib.domain.bidder.Bidder
import org.marketdesignresearch.mechlib.domain.price.LinearPrices
import org.marketdesignresearch.mechlib.domain.price.Price
import java.math.BigDecimal

class GSVMDomainTest {

    @Test
    fun `Should create valid GSVM domain`() {
        val domain = GSVMDomainWrapper(seed = 1234L).toDomain()

        assertThat(domain.efficientAllocation.winners).containsExactlyInAnyOrder(domain.bidders[2], domain.bidders[3], domain.bidders[1], domain.bidders[5])
        assertThat(domain.efficientAllocation.totalAllocationValue).isEqualByComparingTo("427.267118458564016")
        assertThat(domain.efficientAllocation.allocationOf(domain.bidders[0]).value).isEqualByComparingTo("0")
        assertThat(domain.efficientAllocation.allocationOf(domain.bidders[1]).value).isEqualByComparingTo("49.44732980287257")
        assertThat(domain.efficientAllocation.allocationOf(domain.bidders[2]).value).isEqualByComparingTo("28.455878413016116")
        assertThat(domain.efficientAllocation.allocationOf(domain.bidders[3]).value).isEqualByComparingTo("246.45282995483376")
        assertThat(domain.efficientAllocation.allocationOf(domain.bidders[4]).value).isEqualByComparingTo("0")
        assertThat(domain.efficientAllocation.allocationOf(domain.bidders[5]).value).isEqualByComparingTo("102.91108028784157")
        assertThat(domain.efficientAllocation.allocationOf(domain.bidders[6]).value).isEqualByComparingTo("0")

    }

    @Test
    fun `Should create identical domains`() {
        val domain1 = GSVMDomainWrapper(seed = 1L).toDomain()
        val domain2 = GSVMDomainWrapper(seed = 1L).toDomain()
        assertThat(domain1).isEqualTo(domain2)
    }

}