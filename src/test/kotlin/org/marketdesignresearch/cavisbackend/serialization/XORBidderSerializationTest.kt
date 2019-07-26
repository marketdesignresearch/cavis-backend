package org.marketdesignresearch.cavisbackend.serialization

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.marketdesignresearch.mechlib.domain.bidder.XORBidder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

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

    @Test
    fun deserialize() {
        val deserialized = json?.parseObject("{\"id\": \"3e6f4e8f-ac08-4669-b262-22321a3f9fd8\", \"name\": \"B\", \"description\": \"Description of Bidder B\"}")
        assertThat(deserialized?.id).isEqualTo(UUID.fromString("3e6f4e8f-ac08-4669-b262-22321a3f9fd8"))
        assertThat(deserialized?.name).isEqualTo("B")
        assertThat(deserialized?.description).isEqualTo("Description of Bidder B")
    }

}