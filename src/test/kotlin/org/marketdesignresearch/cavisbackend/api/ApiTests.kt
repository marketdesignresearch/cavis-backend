package org.marketdesignresearch.cavisbackend.api

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.*


@SpringBootTest
@AutoConfigureMockMvc
@WithAnonymousUser
class ApiTests {

    private val logger = LoggerFactory.getLogger(ApiTests::class.java)

    @Autowired
    lateinit var wac: WebApplicationContext

    lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build()
    }

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
                .andExpect(jsonPath("$.name").value("TestAuction A"))
                .andExpect(jsonPath("$.seed").isNumber)
                .andExpect(jsonPath("$.tags").isArray)
                .andExpect(jsonPath("$.tags[0]").value("test-generated"))
                .andExpect(jsonPath("$.id").isString)
                .andExpect(jsonPath("$.auctionType").value("SINGLE_ITEM_SECOND_PRICE"))
                .andExpect(jsonPath("$.auctionConfig").exists())
                .andExpect(jsonPath("$.domainConfig").exists())
                .andExpect(jsonPath("$.auction.domain.bidders[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").value("A"))
                .andExpect(jsonPath("$.auction.domain.bidders[1].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").value("B"))
                .andExpect(jsonPath("$.auction.domain.goods[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.goods[0].name").value("item"))
                .andExpect(jsonPath("$.auction.domain.goods[0].quantity").value(1))
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds").isEmpty)
                .andExpect(jsonPath("$.auction.currentPrices").isEmpty)
                .andExpect(jsonPath("$.auction.finished").value(false))
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
        created()
        created()

        mvc.perform(get("/auctions/"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andDo { result -> logger.info(result.response.contentAsString) }
    }

    @Test
    fun `Should delete auctions`() {
        mvc.perform(delete("/auctions/${UUID.randomUUID()}"))
                .andExpect(status().isNotFound)
                .andDo { result -> logger.info(result.response.contentAsString) }

        val created = created()
        val id = created.getString("id")

        mvc.perform(get("/auctions/$id"))
                .andExpect(status().isOk)
                .andDo { result -> logger.info(result.response.contentAsString) }

        mvc.perform(delete("/auctions/$id"))
                .andExpect(status().isNoContent)
                .andDo { result -> logger.info(result.response.contentAsString) }

        mvc.perform(get("/auctions/$id"))
                .andExpect(status().isNotFound)
                .andDo { result -> logger.info(result.response.contentAsString) }
    }

    @Test
    fun `Should run demand query`() {
        val created = created()
        val id = created.getString("id")
        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")
        val itemUuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("goods").getJSONObject(0).getString("id")

        val demandQueryContent = JSONObject()
                .put("prices", JSONObject()
                        .put(itemUuid, 0))
                .put("bidders", JSONArray().put(bidder1Uuid))
                .put("numberOfBundles", 3)

        // No content
        mvc.perform(post("/auctions/$id/demandquery"))
                .andExpect(status().isBadRequest)
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }

        // Empty content
        mvc.perform(
                post("/auctions/$id/demandquery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk) // Zero-prices query for all bidders
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }

        mvc.perform(
                post("/auctions/$id/demandquery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandQueryContent.toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0].hash").isString)
                .andExpect(jsonPath("$.$bidder1Uuid[0].entries[0].good").value(itemUuid))
                .andExpect(jsonPath("$.$bidder1Uuid[0].entries[0].amount").value(1))
                .andExpect(jsonPath("$.$bidder1Uuid[0].entries[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder1Uuid[1].hash").isString)
                .andExpect(jsonPath("$.$bidder1Uuid[1].entries").isEmpty)
                .andExpect(jsonPath("$.$bidder1Uuid[2]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Uuid").doesNotExist())
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }
        mvc.perform(
                post("/auctions/$id/demandquery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandQueryContent.put("bidders", null).toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0].hash").isString)
                .andExpect(jsonPath("$.$bidder1Uuid[0].entries[0].good").value(itemUuid))
                .andExpect(jsonPath("$.$bidder1Uuid[0].entries[0].amount").value(1))
                .andExpect(jsonPath("$.$bidder1Uuid[0].entries[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder1Uuid[1].hash").isString)
                .andExpect(jsonPath("$.$bidder1Uuid[1].entries").isEmpty)
                .andExpect(jsonPath("$.$bidder1Uuid[2]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Uuid").isArray)
                .andExpect(jsonPath("$.$bidder2Uuid[0].hash").isString)
                .andExpect(jsonPath("$.$bidder2Uuid[0].entries[0].good").value(itemUuid))
                .andExpect(jsonPath("$.$bidder2Uuid[0].entries[0].amount").value(1))
                .andExpect(jsonPath("$.$bidder2Uuid[0].entries[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Uuid[1].hash").isString)
                .andExpect(jsonPath("$.$bidder2Uuid[1].entries").isEmpty)
                .andExpect(jsonPath("$.$bidder2Uuid[2]").doesNotExist())
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }

        mvc.perform(
                post("/auctions/$id/demandquery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject().put("prices", JSONObject().put(itemUuid, 500000)).toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0].hash").isString)
                .andExpect(jsonPath("$.$bidder1Uuid[0].entries").isEmpty)
                .andExpect(jsonPath("$.$bidder1Uuid[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0].hash").isString)
                .andExpect(jsonPath("$.$bidder2Uuid[0].entries").isEmpty)
                .andExpect(jsonPath("$.$bidder2Uuid[1]").doesNotExist())
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }
    }

    @Test
    fun `Should run value query`() {
        val created = created()
        val id = created.getString("id")
        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")
        val itemUuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("goods").getJSONObject(0).getString("id")

        val valueQueryContent = JSONObject()
                .put("bundles", JSONArray()
                        .put(JSONObject().put(itemUuid, 1)))
                .put("bidders", JSONArray().put(bidder1Uuid))

        // No content
        mvc.perform(post("/auctions/$id/valuequery"))
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }
                .andExpect(status().isBadRequest)

        // Empty content
        mvc.perform(
                post("/auctions/$id/valuequery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }
                .andExpect(status().isBadRequest)

        mvc.perform(
                post("/auctions/$id/valuequery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valueQueryContent.toString()))
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid[0].value").isNumber)
                .andExpect(jsonPath("$.$bidder1Uuid[0].bundle.hash").isString)
                .andExpect(jsonPath("$.$bidder1Uuid[0].bundle.entries[0].good").isString)
                .andExpect(jsonPath("$.$bidder1Uuid[0].bundle.entries[0].amount").isNumber)
                .andExpect(jsonPath("$.$bidder1Uuid[0].bundle.entries[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder1Uuid[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Uuid").doesNotExist())

        mvc.perform(
                post("/auctions/$id/valuequery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valueQueryContent.put("bidders", null).toString()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isArray)
                .andExpect(jsonPath("$.$bidder2Uuid").isArray)
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }

        mvc.perform(
                post("/auctions/$id/valuequery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bundles\": []}"))
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Uuid").isArray)
                .andExpect(jsonPath("$.$bidder1Uuid").isEmpty)
                .andExpect(jsonPath("$.$bidder2Uuid").isArray)
                .andExpect(jsonPath("$.$bidder2Uuid").isEmpty)
    }

    @Test
    fun `Should place bids`() {
        val created = created()
        val id = created.getString("id")
        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")
        val itemUuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("goods").getJSONObject(0).getString("id")

        mvc.perform(
                post("/auctions/$id/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bids(bidder1Uuid, bidder2Uuid, itemUuid).toString()))
                .andDo { logger.info("Response: {}", it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.auction.rounds").isEmpty)

        mvc.perform(post("/auctions/$id/close-round"))
                .andDo { logger.info("Response: {}", it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
                .andExpect(jsonPath("$.auction.rounds[0].outcome").exists())
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder2Uuid.value").value(12))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder2Uuid.bundle.hash").isString)
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder2Uuid.bundle.entries[0].good").value(itemUuid))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder2Uuid.bundle.entries[0].amount").value(1))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.payments.totalPayments").value(10))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.payments.$bidder2Uuid").value(10))
    }

    @Test
    fun `Should reset auction`() {
        val body = JSONObject()
                .put("domain", JSONObject()
                        .put("type", "gsvm"))
                .put("auctionType", "CCA")
        val created = JSONObject(mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andReturn().response.contentAsString)
        val id = created.getString("id")

        for (i in 0..4) {
            mvc.perform(post("/auctions/$id/advance-round"))
                    .andExpect(status().isOk)
        }

        mvc.perform(get("/auctions/$id/"))
                .andDo { logger.info("Response: {}", it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds[0].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[1].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[2].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[3].bids").exists())
                .andExpect(jsonPath("$.auction.rounds[4].bids").exists())

        mvc.perform(
                put("/auctions/$id/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"round\": 3}"))
                .andDo { logger.info("Response: {}", it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)

        mvc.perform(get("/auctions/$id/"))
                .andDo { logger.info("Response: {}", it.response.contentAsString) }
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
        mvc.perform(get("/auctions/${finished()}/result/"))
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.socialWelfare").value(12))
                .andExpect(jsonPath("$.revenue").value(10))
                .andExpect(jsonPath("$.winnerUtilities").exists())
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
                    .put("goods", JSONArray().put(JSONObject().put("name", "item"))))
            .put("auctionType", "SINGLE_ITEM_SECOND_PRICE")
            .put("name", "TestAuction A")
            .put("tags", JSONArray().put("test-generated"))

    private fun created(): JSONObject = JSONObject(mvc.perform(
            post("/auctions/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body().toString()))
            .andDo { logger.info(it.response.contentAsString)}
            .andReturn().response.contentAsString)

    private fun bids(bidder1Uuid: String, bidder2Uuid: String, itemUuid: String): JSONObject = JSONObject()
            .put(bidder1Uuid, JSONArray().put(JSONObject().put("amount", 10).put("bundle", JSONObject().put(itemUuid, 1))))
            .put(bidder2Uuid, JSONArray().put(JSONObject().put("amount", 12).put("bundle", JSONObject().put(itemUuid, 1))))

    private fun finished(): String {
        val created = created()
        val id = created.getString("id")
        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id")
        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id")
        val itemUuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("goods").getJSONObject(0).getString("id")

        mvc.perform(
                post("/auctions/$id/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bids(bidder1Uuid, bidder2Uuid, itemUuid).toString()))
                .andExpect(status().isOk)
        mvc.perform(post("/auctions/$id/close-round"))
                .andExpect(status().isOk)
        return id
    }
}

