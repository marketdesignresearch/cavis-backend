package org.marketdesignresearch.cavisbackend.serialization

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.marketdesignresearch.mechlib.core.bidder.XORBidder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import org.springframework.test.context.junit4.SpringRunner

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
        assertThat(serialized?.json).isEqualTo(
                "{" +
                    "\"id\":\"${bidder.id}\"," +
                    "\"name\":\"A\"," +
                    "\"description\":\"${bidder.description}\"," +
                    "\"shortDescription\":\"XOR-Bidder: A\"" +
                "}"
        )
    }

}