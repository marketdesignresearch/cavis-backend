package org.marketdesignresearch.cavisbackend.serialization

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.marketdesignresearch.mechlib.core.SimpleGood
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import org.springframework.test.context.junit4.SpringRunner


@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@JsonTest
@RunWith(SpringRunner::class)
class SimpleGoodSerializationTest {

    @Autowired
    private val json: JacksonTester<SimpleGood>? = null

    @Test
    fun serialize() {
        val good = SimpleGood("A")
        val serialized = json?.write(good)

        assertThat(serialized?.json).isEqualTo("{\"name\":\"A\",\"id\":\"${good.uuid}\",\"quantity\":1}")
    }

    @Test
    fun deserialize() {
        val serialized = "{\"name\":\"A\"}"
        val deserialized = json?.parseObject(serialized)
        assertThat(deserialized?.name).isEqualTo("A")
    }

}