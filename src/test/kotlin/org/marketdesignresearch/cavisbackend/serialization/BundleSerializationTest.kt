package org.marketdesignresearch.cavisbackend.serialization

import org.junit.Test
import org.junit.runner.RunWith
import org.marketdesignresearch.mechlib.domain.Bundle
import org.marketdesignresearch.mechlib.domain.SimpleGood
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import org.springframework.test.context.junit4.SpringRunner
import org.assertj.core.api.Assertions.*


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
        val bundle = Bundle.singleGoods(setOf(itemA, itemB))
        val serialized = json?.write(bundle)

        assertThat(serialized?.json).isEqualTo("{\"bundleEntries\":[{\"good\":{\"id\":\"B\",\"availability\":1,\"dummyGood\":false},\"amount\":1},{\"good\":{\"id\":\"A\",\"availability\":1,\"dummyGood\":false},\"amount\":1}],\"singleGood\":null}")
    }

    @Test
    fun deserialize() {
        val serialized = "{\"bundleEntries\":[{\"good\":{\"id\":\"B\",\"availability\":1,\"dummyGood\":false},\"amount\":1},{\"good\":{\"id\":\"A\",\"availability\":1,\"dummyGood\":false},\"amount\":1}],\"singleGood\":null}"
        val deserialized = json?.parseObject(serialized)
        assertThat(deserialized?.bundleEntries?.size).isEqualTo(2)
    }

}