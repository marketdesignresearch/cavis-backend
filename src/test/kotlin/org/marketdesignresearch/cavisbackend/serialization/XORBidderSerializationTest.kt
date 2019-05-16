package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import org.junit.runner.RunWith
import org.marketdesignresearch.mechlib.domain.Bundle
import org.marketdesignresearch.mechlib.domain.SimpleGood
import org.marketdesignresearch.mechlib.domain.bidder.XORBidder
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.boot.test.json.JacksonTester
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest

import org.assertj.core.api.Assertions.*

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@JsonTest
@RunWith(SpringRunner::class)
class XORBidderSerializationTest {

    @Autowired
    private val json: JacksonTester<XORBidder>? = null

    @Test
    fun serialize() {
        val bidder = XORBidder("A")
        val serialized = json?.write(bidder)
        assertThat(serialized?.json).isEqualTo("{\"id\":\"A\",\"value\":{\"bundleValues\":[]}}")
    }

    @Test
    fun deserialize() {
        val deserialized = json?.parseObject("{\"id\": \"A\"}")
        assertThat(deserialized?.id).isEqualTo("A")
    }

}