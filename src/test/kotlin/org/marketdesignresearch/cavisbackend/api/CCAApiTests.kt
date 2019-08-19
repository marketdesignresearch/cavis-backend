package org.marketdesignresearch.cavisbackend.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class CCAApiTests {

    private val logger = LoggerFactory.getLogger(CCAApiTests::class.java)

    @Autowired
    lateinit var mvc: MockMvc

    val body: JSONObject = JSONObject()
            .put("domain", JSONObject()
                    .put("type", "additiveValue")
                    .put("bidders", JSONArray()
                            .put(JSONObject()
                                    .put("name", "1"))
                            .put(JSONObject()
                                    .put("name", "2"))
                            .put(JSONObject()
                                    .put("name", "3")))
                    .put("goods", JSONArray()
                            .put(JSONObject().put("name", "A"))
                            .put(JSONObject().put("name", "B"))))
            .put("auctionType", "CCA")

    @Test
    fun `Should create new CCA auction`() {

        var id: String? = null
        var content: String? = null
        var bidder1Id: String? = null
        var bidder2Id: String? = null
        var bidder3Id: String? = null
        var item1Id: String? = null
        var item2Id: String? = null

        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk)
                .andDo {
                    content = it.response.contentAsString
                    val json = JSONObject(content)
                    id = json.getString("id")
                    val bidderArray = json.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders")
                    val goodsArray = json.getJSONObject("auction").getJSONObject("domain").getJSONArray("goods")
                    bidder1Id = bidderArray.getJSONObject(0).getString("id")
                    bidder2Id = bidderArray.getJSONObject(1).getString("id")
                    bidder3Id = bidderArray.getJSONObject(2).getString("id")
                    item1Id = goodsArray.getJSONObject(0).getString("id")
                    item2Id = goodsArray.getJSONObject(1).getString("id")
                }

        mvc.perform(get("/auctions/$id"))
                .andExpect(status().isOk)
                .andDo {
                    logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString)
                    assertThat(content).isEqualTo(it.response.contentAsString)
                }
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isString)
                .andExpect(jsonPath("$.auctionType").value("CCA"))
                .andExpect(jsonPath("$.auction.domain").exists())
                .andExpect(jsonPath("$.auction.domain.bidders").exists())
                .andExpect(jsonPath("$.auction.domain.bidders[0].id").value(bidder1Id!!))
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").value("1"))
                .andExpect(jsonPath("$.auction.domain.bidders[1].id").value(bidder2Id!!))
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").value("2"))
                .andExpect(jsonPath("$.auction.domain.bidders[2].id").value(bidder3Id!!))
                .andExpect(jsonPath("$.auction.domain.bidders[2].name").value("3"))
                .andExpect(jsonPath("$.auction.domain.goods").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0]").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0].id").value(item1Id!!))
                .andExpect(jsonPath("$.auction.domain.goods[0].name").value("A"))
                .andExpect(jsonPath("$.auction.domain.goods[0].quantity").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[1]").exists())
                .andExpect(jsonPath("$.auction.domain.goods[1].id").value(item2Id!!))
                .andExpect(jsonPath("$.auction.domain.goods[1].name").value("B"))
                .andExpect(jsonPath("$.auction.domain.goods[1].quantity").value(1))
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds").isEmpty)
                .andExpect(jsonPath("$.auction.currentPrices").exists())
                .andExpect(jsonPath("$.auction.currentPrices.$item1Id").isNumber)
                .andExpect(jsonPath("$.auction.currentPrices.$item2Id").isNumber)
                .andExpect(jsonPath("$.auction.currentRoundType").value("Clock Round"))
                .andExpect(jsonPath("$.auction.supplementaryRounds").isArray)

        mvc.perform(
                post("/auctions/$id/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject()
                                .put(bidder1Id, JSONArray().put(JSONObject().put("amount", 2).put("bundle", JSONObject().put(item2Id, 1))))
                                .put(bidder2Id, JSONArray().put(JSONObject().put("amount", 3).put("bundle", JSONObject().put(item1Id, 1)))).toString()))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id!!))
                .andExpect(jsonPath("$.auction.rounds").isEmpty)

        mvc.perform(post("/auctions/$id/close-round"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id!!))
                .andExpect(jsonPath("$.auction.currentRoundType").value("Supplementary Round"))
                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
                .andExpect(jsonPath("$.auction.rounds[0].outcome").exists())
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder2Id.value").value(3))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder2Id.bundle.hash").isString)
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder2Id.bundle.entries[0].good").value(item1Id!!))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder2Id.bundle.entries[0].amount").value(1))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder1Id.value").value(2))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder1Id.bundle.hash").isString)
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder1Id.bundle.entries[0].good").value(item2Id!!))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.allocation.$bidder1Id.bundle.entries[0].amount").value(1))
                .andExpect(jsonPath("$.auction.rounds[0].outcome.payments.totalPayments").value(0))
                .andExpect(jsonPath("$.auction.rounds[0].type").value("CLOCK"))
                .andExpect(jsonPath("$.auction.rounds[0].overDemand.$item1Id").value(0))
                .andExpect(jsonPath("$.auction.rounds[0].overDemand.$item2Id").value(0))

    }

    @Test
    fun `Should create new CCA auction, finish clock phase and then and jump to end`() {

        var id: String? = null

        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk)
                .andDo {id = JSONObject(it.response.contentAsString).getString("id") }

        val afterPhase = mvc.perform(post("/auctions/$id/advance-phase"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andReturn().response.contentAsString

        val check = mvc.perform(get("/auctions/$id"))
                .andExpect(status().isOk)
                .andReturn().response.contentAsString

        assertThat(afterPhase).isEqualTo(check)

        val finished = mvc.perform(post("/auctions/$id/finish"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andReturn().response.contentAsString

        // More rounds
        assertThat(afterPhase).isNotEqualTo(finished)

    }

    @Test
    fun `Should create new CCA auction, finish clock phase and then retrieve an intermediate allocation`() {

        var id: String? = null

        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk)
                .andDo {id = JSONObject(it.response.contentAsString).getString("id") }

        val finished = mvc.perform(post("/auctions/$id/finish"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andReturn().response.contentAsString

        val check = mvc.perform(get("/auctions/$id"))
                .andExpect(status().isOk)
                .andReturn().response.contentAsString

        assertThat(finished).isEqualTo(check)

        val resultRound3 = JSONObject(finished).getJSONObject("auction").getJSONArray("rounds")
                .getJSONObject(2)
        assertThatThrownBy { resultRound3.getJSONObject("mechanismResult") }.isExactlyInstanceOf(JSONException::class.java)

        val result = JSONObject(mvc.perform(get("/auctions/$id/rounds/2/result"))
                .andReturn().response.contentAsString)

        val newResultRound3 = JSONObject(mvc.perform(get("/auctions/$id/")).andReturn().response.contentAsString)
                .getJSONObject("auction").getJSONArray("rounds").getJSONObject(2).getJSONObject("outcome")

        assertThat(result.toString()).isEqualTo(newResultRound3.toString())

    }
}

