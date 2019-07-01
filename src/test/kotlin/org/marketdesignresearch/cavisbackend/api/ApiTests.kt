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
class ApiTests {

    private val logger = LoggerFactory.getLogger(ApiTests::class.java)

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun `Unknown UUID should return 404`() {
        mvc.perform(get("/auctions/ded9ac81-bc30-4d3b-81a7-946bde6aa7de"))
                .andExpect(status().isNotFound)
    }

    @Test
    fun `Should create new auction`() {

        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.uuid").exists())
                .andExpect(jsonPath("$.uuid").isString)
                .andExpect(jsonPath("$.auctionType").value("SINGLE_ITEM_SECOND_PRICE"))
                .andExpect(jsonPath("$.auction.domain").exists())
                .andExpect(jsonPath("$.auction.domain.bidders").exists())
                .andExpect(jsonPath("$.auction.domain.bidders[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").value("A"))
                .andExpect(jsonPath("$.auction.domain.bidders[1].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").value("B"))
                .andExpect(jsonPath("$.auction.domain.goods").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0]").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.goods[0].id").value("item"))
                .andExpect(jsonPath("$.auction.domain.goods[0].availability").isNumber)
                .andExpect(jsonPath("$.auction.domain.goods[0].availability").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[0].dummyGood").isBoolean)
                .andExpect(jsonPath("$.auction.domain.goods[0].dummyGood").value(false))
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds").isEmpty)
                .andDo { result -> logger.info(result.response.contentAsString) }

    }

    @Test
    fun `Should fail to create new auction`() {

        // Bad media type
        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body().toString()))
                .andExpect(status().isUnsupportedMediaType)

        // Auction type missing
        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().remove("auctionType").toString()))
                .andExpect(status().isBadRequest)

        // DomainWrapper type missing
        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("domain", body().getJSONObject("domain").remove("type")).toString()))
                .andExpect(status().isBadRequest)

    }

    @Test
    fun `Should get auctions`() {
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
    fun `Should run demand query`() {
        val created = created()
        val uuid = created.getString("uuid")
        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")

        val demandQueryContent = JSONObject()
                .put("prices", JSONObject()
                        .put("item", 0))
                .put("bidders", JSONArray().put(bidder1Uuid))
                .put("numberOfBundles", 3)

        // No content
        mvc.perform(post("/auctions/$uuid/demandquery"))
                .andExpect(status().isBadRequest)
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }

        // Empty content
        mvc.perform(
                post("/auctions/$uuid/demandquery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk) // Zero-prices query for all bidders
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }

        mvc.perform(
                post("/auctions/$uuid/demandquery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandQueryContent.toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0]").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder1Uuid[0][0].good").value("item"))
                .andExpect(jsonPath("$.$bidder1Uuid[0][0].amount").value(1))
                .andExpect(jsonPath("$.$bidder1Uuid[0][1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Uuid").doesNotExist())
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }
        mvc.perform(
                post("/auctions/$uuid/demandquery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandQueryContent.put("bidders", null).toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0]").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder1Uuid[0][0].good").value("item"))
                .andExpect(jsonPath("$.$bidder1Uuid[0][0].amount").value(1))
                .andExpect(jsonPath("$.$bidder1Uuid[0][1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Uuid").isArray)
                .andExpect(jsonPath("$.$bidder2Uuid[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Uuid[0][0].good").value("item"))
                .andExpect(jsonPath("$.$bidder2Uuid[0][0].amount").value(1))
                .andExpect(jsonPath("$.$bidder2Uuid[0][1]").doesNotExist())
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }

        mvc.perform(
                post("/auctions/$uuid/demandquery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject().put("prices", JSONObject().put("item", 500000)).toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0]").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0]").isEmpty)
                .andExpect(jsonPath("$.$bidder1Uuid[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0]").isArray)
                .andExpect(jsonPath("$.$bidder2Uuid[0]").isEmpty)
                .andExpect(jsonPath("$.$bidder2Uuid[1]").doesNotExist())
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }
    }

    @Test
    fun `Should run value query`() {
        val created = created()
        val uuid = created.getString("uuid")
        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")

        val valueQueryContent = JSONObject()
                .put("bundle", JSONObject()
                        .put("item", 1))
                .put("bidders", JSONArray().put(bidder1Uuid))

        // No content
        mvc.perform(post("/auctions/$uuid/valuequery"))
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }
                .andExpect(status().isBadRequest)

        // Empty content
        mvc.perform(
                post("/auctions/$uuid/valuequery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }
                .andExpect(status().isBadRequest)

        mvc.perform(
                post("/auctions/$uuid/valuequery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valueQueryContent.toString()))
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isNumber)
                .andExpect(jsonPath("$.$bidder2Uuid").doesNotExist())

        mvc.perform(
                post("/auctions/$uuid/valuequery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valueQueryContent.put("bidders", null).toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isNumber)
                .andExpect(jsonPath("$.$bidder2Uuid").isNumber)
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }

        mvc.perform(
                post("/auctions/$uuid/valuequery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bundle\": {}}"))
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isNumber)
                .andExpect(jsonPath("$.$bidder2Uuid").isNumber)
    }

    @Test
    fun `Should place bids`() {
        val created = created()
        val uuid = created.getString("uuid")
        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")

        mvc.perform(
                post("/auctions/$uuid/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bids(bidder1Uuid, bidder2Uuid).toString()))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.uuid").value(uuid))
                .andExpect(jsonPath("$.auction.rounds").isEmpty)

        mvc.perform(post("/auctions/$uuid/close-round"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.uuid").value(uuid))
                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult").exists())
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$bidder2Uuid.value").value(12))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$bidder2Uuid.goods.item").value(1))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.totalPayments").value(10))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.$bidder2Uuid").value(10))
    }

    @Test
    fun `Should reset auction`() {
        val created = created()
        val uuid = created.getString("uuid")
        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")

        for (i in 0..4) {
            mvc.perform(
                    post("/auctions/$uuid/bids")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bids(bidder1Uuid, bidder2Uuid).toString()))
                    .andExpect(status().isOk)
            mvc.perform(post("/auctions/$uuid/close-round"))
                    .andExpect(status().isOk)
        }

        mvc.perform(get("/auctions/$uuid/"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
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
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
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
    fun `Should get result`() {
        mvc.perform(get("/auctions/${finished()}/result"))
                .andDo { result -> logger.info("Request: {} | Response: {}", result.request.contentAsString, result.response.contentAsString) }
                .andExpect(status().isOk)
                //.andExpect(jsonPath("$.allocation.B.value").value(12))
                //.andExpect(jsonPath("$.allocation.B.goods.item").value(1))
                //.andExpect(jsonPath("$.payments.B").value(10))
                .andExpect(jsonPath("$.payments.totalPayments").value(10))
    }

    private fun body(): JSONObject = JSONObject()
            .put("domain", JSONObject()
                    .put("type", "unitDemandValue")
                    .put("bidders", JSONArray()
                            .put(JSONObject()
                                    .put("name", "A"))
                            .put(JSONObject()
                                    .put("name", "B")))
                    .put("goods", JSONArray().put(JSONObject().put("id", "item"))))
            .put("auctionType", "SINGLE_ITEM_SECOND_PRICE")

    private fun created(): JSONObject = JSONObject(mvc.perform(
            post("/auctions/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body().toString()))
            .andReturn().response.contentAsString)

    private fun bids(bidder1Uuid: String, bidder2Uuid: String): JSONObject = JSONObject()
            .put(bidder1Uuid, JSONArray().put(JSONObject().put("amount", 10).put("bundle", JSONObject().put("item", 1))))
            .put(bidder2Uuid, JSONArray().put(JSONObject().put("amount", 12).put("bundle", JSONObject().put("item", 1))))

    private fun finished(): String {
        val created = created()
        val uuid = created.getString("uuid")
        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")

        mvc.perform(
                post("/auctions/$uuid/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bids(bidder1Uuid, bidder2Uuid).toString()))
                .andExpect(status().isOk)
        mvc.perform(post("/auctions/$uuid/close-round"))
                .andExpect(status().isOk)
        return uuid
    }
}
