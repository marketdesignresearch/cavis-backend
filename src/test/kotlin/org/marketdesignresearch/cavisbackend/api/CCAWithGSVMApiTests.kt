package org.marketdesignresearch.cavisbackend.api

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@AutoConfigureMockMvc
class CCAWithGSVMApiTests {

    private val logger = LoggerFactory.getLogger(CCAWithGSVMApiTests::class.java)

    @Autowired
    lateinit var wac: WebApplicationContext

    lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build()
    }

    @Test
    fun `Should create new CCA auction in GSVM domain`() {

        var id: String? = null
        var content: String? = null
        var bidder1Id: String? = null
        var bidder2Id: String? = null
        var bidder3Id: String? = null
        var item1Id: String? = null
        var item2Id: String? = null

        val body = JSONObject()
                .put("domain", JSONObject()
                        .put("type", "gsvm"))
                .put("auctionType", "CCA")

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
                    logger.info("Response: {}", it.response.contentAsString)
                    assertThat(content).isEqualTo(it.response.contentAsString)
                }
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isString)
                .andExpect(jsonPath("$.auctionType").value("CCA"))
                .andExpect(jsonPath("$.auction.domain").exists())
                .andExpect(jsonPath("$.auction.domain.bidders").exists())
                .andExpect(jsonPath("$.auction.domain.bidders[0].id").value(bidder1Id!!))
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").value("0"))
                .andExpect(jsonPath("$.auction.domain.bidders[1].id").value(bidder2Id!!))
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").value("1"))
                .andExpect(jsonPath("$.auction.domain.bidders[2].id").value(bidder3Id!!))
                .andExpect(jsonPath("$.auction.domain.bidders[2].name").value("2"))
                .andExpect(jsonPath("$.auction.domain.goods").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0]").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0].id").value(item1Id!!))
                .andExpect(jsonPath("$.auction.domain.goods[0].name").value("A"))
                .andExpect(jsonPath("$.auction.domain.goods[0].quantity").isNumber)
                .andExpect(jsonPath("$.auction.domain.goods[0].quantity").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[1]").exists())
                .andExpect(jsonPath("$.auction.domain.goods[1].id").value(item2Id!!))
                .andExpect(jsonPath("$.auction.domain.goods[1].name").value("B"))
                .andExpect(jsonPath("$.auction.domain.goods[1].quantity").isNumber)
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
                .andDo { logger.info("Response: {}", it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id!!))
                .andExpect(jsonPath("$.auction.rounds").isEmpty)

        mvc.perform(post("/auctions/$id/close-round"))
                .andDo { logger.info("Response: {}", it.response.contentAsString) }
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
}

