package org.marketdesignresearch.cavisbackend.api

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class VCGAPITests {

    private val logger = LoggerFactory.getLogger(VCGAPITests::class.java)

    @Autowired
    lateinit var mvc: MockMvc
    @Test
    fun `Should create new VCG auction`() {

        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().toString()))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isString)
                .andExpect(jsonPath("$.auctionType").value("VCG_XOR"))
                .andExpect(jsonPath("$.auction.domain.bidders[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").value("1"))
                .andExpect(jsonPath("$.auction.domain.bidders[1].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").value("2"))
                .andExpect(jsonPath("$.auction.domain.bidders[2].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[2].name").value("3"))
                .andExpect(jsonPath("$.auction.domain.goods[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.goods[0].name").value("A"))
                .andExpect(jsonPath("$.auction.domain.goods[1].id").isString)
                .andExpect(jsonPath("$.auction.domain.goods[1].name").value("B"))
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds").isEmpty)
                .andExpect(jsonPath("$.auction.currentPrices").exists())
                .andExpect(jsonPath("$.auction.currentPrices").isEmpty)
                .andExpect(jsonPath("$.auction.finished").value(false))

    }

    @Test
    fun `Should handle LLG bids correctly`() {
        val created = created()
        val id = created.getString("id")
        val b1Id = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val b2Id = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")
        val b3Id = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(2).getString("id")
        val AId = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("goods").getJSONObject(0).getString("id")
        val BId = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("goods").getJSONObject(1).getString("id")

        mvc.perform(
                post("/auctions/$id/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject()
                                .put(b1Id, JSONArray().put(JSONObject().put("amount", 10).put("bundle", JSONObject().put(AId, 1))))
                                .put(b2Id, JSONArray().put(JSONObject().put("amount", 10).put("bundle", JSONObject().put(BId, 1))))
                                .put(b3Id, JSONArray().put(JSONObject().put("amount", 12).put("bundle", JSONObject().put(AId, 1).put(BId, 1)))).toString()))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.auction.rounds").isEmpty)

        mvc.perform(post("/auctions/$id/close-round"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b1Id.value").value(10))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b1Id.bundle.hash").isNumber)
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b1Id.bundle.entries[0].good").value(AId))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b1Id.bundle.entries[0].amount").value(1))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b2Id.value").value(10))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b2Id.bundle.hash").isNumber)
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b2Id.bundle.entries[0].good").value(BId))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b2Id.bundle.entries[0].amount").value(1))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.totalPayments").value(4))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.$b1Id").value(2))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.$b2Id").value(2))
    }

    @Test
    fun `Should correctly advance round`() {
        val created = created()
        val id = created.getString("id")
        val b1Id = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val b2Id = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")
        val b3Id = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(2).getString("id")
        val AId = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("goods").getJSONObject(0).getString("id")
        val BId = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("goods").getJSONObject(1).getString("id")

        val bids = mvc.perform(
                post("/auctions/$id/propose"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andReturn().response.contentAsString

        val bidArray = JSONArray(bids)

        mvc.perform(post("/auctions/$id/finish"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b2Id.value").value(4))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$b3Id.value").value(5))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.totalPayments").value(6))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.$b2Id").value(3))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.$b3Id").value(3))
                .andExpect(jsonPath("$.auction.rounds[1]").doesNotExist())
    }

    private fun body(): JSONObject = JSONObject()
            .put("domain", JSONObject()
                    .put("type", "unitDemandValue")
                    .put("bidders", JSONArray()
                            .put(JSONObject()
                                    .put("name", "1")
                                    .put("min", 3)
                                    .put("max", 3))
                            .put(JSONObject()
                                    .put("name", "2")
                                    .put("min", 4)
                                    .put("max", 4))
                            .put(JSONObject()
                                    .put("name", "3")
                                    .put("min", 5)
                                    .put("max", 5)))
                    .put("goods", JSONArray()
                            .put(JSONObject().put("name", "A"))
                            .put(JSONObject().put("name", "B"))))
            .put("auctionType", "VCG_XOR")

    private fun created(): JSONObject = JSONObject(mvc.perform(
            post("/auctions/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body().toString()))
            .andReturn().response.contentAsString)

    private fun bids(bidder1Uuid: String, bidder2Uuid: String, itemUuid: String): JSONObject = JSONObject()
            .put(bidder1Uuid, JSONArray().put(JSONObject().put("amount", 10).put("bundle", JSONObject().put(itemUuid, 1))))
            .put(bidder2Uuid, JSONArray().put(JSONObject().put("amount", 12).put("bundle", JSONObject().put(itemUuid, 1))))
}

