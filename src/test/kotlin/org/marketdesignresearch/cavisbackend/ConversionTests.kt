package org.marketdesignresearch.cavisbackend

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.convert.ConversionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@SpringBootTest
class ConversionTests {
    @Autowired
    lateinit var conversionService: ConversionService

    @Test
    fun whenConvertStringToIntegerUsingDefaultConverter_thenSuccess() {
        assertThat(
                conversionService.convert("25", Int::class.java)).isEqualTo(25)
    }
}