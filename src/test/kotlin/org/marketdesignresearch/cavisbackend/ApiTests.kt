package org.marketdesignresearch.cavisbackend

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ApiTests {

    private val logger = LoggerFactory.getLogger(ApiTests::class.java)

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun unknownUuidShouldReturn404() {
        mvc.perform(get("/auctions/ded9ac81-bc30-4d3b-81a7-946bde6aa7de"))
                .andExpect(status().isNotFound)
    }

    @Test
    fun shouldCreateNewAuction() {

        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.uuid").isString)
                .andExpect(jsonPath("$.auction.domain").exists())
                .andExpect(jsonPath("$.auction.domain.bidders").exists())
                .andExpect(jsonPath("$.auction.domain.bidders[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[0].id").value("A"))
                .andExpect(jsonPath("$.auction.domain.bidders[1].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[1].id").value("B"))
                .andExpect(jsonPath("$.auction.domain.goods").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0]").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.goods[0].id").value("item"))
                .andExpect(jsonPath("$.auction.domain.goods[0].availability").isNumber)
                .andExpect(jsonPath("$.auction.domain.goods[0].availability").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[0].dummyGood").isBoolean)
                .andExpect(jsonPath("$.auction.domain.goods[0].dummyGood").value(false))
                .andExpect(jsonPath("$.auction.mechanismType").value("SINGLE_ITEM_SECOND_PRICE"))
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds").isEmpty)
                .andDo { result -> logger.info(result.response.contentAsString) }

    }

    @Test
    fun shouldFailToCreateNewAuction() {

        // Bad media type
        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body().toString()))
                .andExpect(status().isUnsupportedMediaType)

        // MechanismType missing
        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().remove("mechanismType").toString()))
                .andExpect(status().isBadRequest)

        // DomainWrapper type missing
        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("domain", body().getJSONObject("domain").remove("type")).toString()))
                .andExpect(status().isBadRequest)

    }

    @Test
    fun shouldGetAuctions() {
        val created1 = created()
        val uuid1 = created1.getString("uuid")

        val created2 = created()
        val uuid2 = created2.getString("uuid")

        mvc.perform(get("/auctions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andDo { result -> logger.info(result.response.contentAsString) }
    }

    @Test
    fun shouldPlaceBids() {
        val created = created()
        val uuid = created.getString("uuid")

        mvc.perform(
                post("/auctions/$uuid/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bids().toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.uuid").value(uuid))
                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
                .andExpect(jsonPath("$.auction.rounds[0].auctionResult").exists())
                .andExpect(jsonPath("$.auction.rounds[0].auctionResult.allocation.B.value").value(12))
                .andExpect(jsonPath("$.auction.rounds[0].auctionResult.allocation.B.goods.item").value(1))
                .andExpect(jsonPath("$.auction.rounds[0].auctionResult.payments.totalPayments").value(10))
                .andExpect(jsonPath("$.auction.rounds[0].auctionResult.payments.B").value(10))
                .andDo { result -> logger.info(result.response.contentAsString) }
    }

    @Test
    fun `Should reset auction`() {
        val created = created()
        val uuid = created.getString("uuid")

        for (i in 0..4) {
            mvc.perform(
                    post("/auctions/$uuid/bids")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bids().toString()))
                    .andExpect(status().isOk)
        }

        mvc.perform(get("/auctions/$uuid/"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds[0].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[1].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[2].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[3].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[4].bids").exists())

        mvc.perform(
                put("/auctions/$uuid/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"round\": 3}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds[0].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[1].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[2].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[3].bids").doesNotExist())
                .andExpect(jsonPath("$.auction.rounds[4].bids").doesNotExist())


    }

    @Test
    fun shouldGetResult() {
        mvc.perform(get("/auctions/${finished()}/result"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.allocation.B.value").value(12))
                .andExpect(jsonPath("$.allocation.B.goods.item").value(1))
                .andExpect(jsonPath("$.payments.totalPayments").value(10))
                .andExpect(jsonPath("$.payments.B").value(10))
                .andDo { result -> logger.info(result.response.contentAsString) }
    }

    private fun body(): JSONObject = JSONObject()
            .put("domain", JSONObject()
                    .put("type", "simple")
                    .put("bidders", JSONArray().put(JSONObject().put("id", "A")).put(JSONObject().put("id", "B")))
                    .put("goods", JSONArray().put(JSONObject().put("id", "item"))))
            .put("mechanismType", "SINGLE_ITEM_SECOND_PRICE")

    private fun created(): JSONObject = JSONObject(mvc.perform(
            post("/auctions/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body().toString()))
            .andReturn().response.contentAsString)

    private fun bids(): JSONObject = JSONObject()
            .put("A", JSONArray().put(JSONObject().put("amount", 10).put("bundle", JSONObject().put("item", 1))))
            .put("B", JSONArray().put(JSONObject().put("amount", 12).put("bundle", JSONObject().put("item", 1))))

    private fun finished(): String {
        val uuid = created().getString("uuid")
        mvc.perform(
                post("/auctions/$uuid/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bids().toString()))
                .andExpect(status().isOk)
        return uuid
    }
}

