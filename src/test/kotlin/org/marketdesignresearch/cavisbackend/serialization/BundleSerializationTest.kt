package org.marketdesignresearch.cavisbackend.serialization

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.marketdesignresearch.cavisbackend.sha256Hex
import org.marketdesignresearch.mechlib.core.Bundle
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import org.springframework.test.context.junit4.SpringRunner


@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@JsonTest
@RunWith(SpringRunner::class)
class BundleSerializationTest {

    @Autowired
    private val json: JacksonTester<Bundle>? = null

    @Test
    fun serialize() {
        val itemA = SimpleGood("A")
        val itemB = SimpleGood("B")
        val bundle = Bundle.of(itemA, itemB)
        val hash = bundle.sha256Hex()
        val serialized = json?.write(bundle)

        assertThat(serialized?.json).isEqualTo("{\"hash\":\"$hash\",\"entries\":[{\"good\":\"${itemA.uuid}\",\"amount\":1},{\"good\":\"${itemB.uuid}\",\"amount\":1}]}")
    }

}