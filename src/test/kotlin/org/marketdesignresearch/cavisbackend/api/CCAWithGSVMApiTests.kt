package org.marketdesignresearch.cavisbackend.api

import org.json.JSONArray
import org.json.JSONObject
import org.junit.BeforeClass
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
import org.assertj.core.api.Assertions.*
import org.marketdesignresearch.mechlib.auction.cca.CCARound

@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CCAWithGSVMApiTests {

    private val logger = LoggerFactory.getLogger(CCAWithGSVMApiTests::class.java)

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun `Should create new CCA auction in GSVM domain`() {

        var id: String? = null
        var content: String? = null
        var bidder1Id: String? = null
        var bidder2Id: String? = null

        val body = JSONObject()
                .put("domain", JSONObject()
                        .put("type", "gsvm"))
                .put("auctionType", "CCA_VCG")

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
                    bidder1Id = bidderArray.getJSONObject(0).getString("id")
                    bidder2Id = bidderArray.getJSONObject(1).getString("id")
                }

        mvc.perform(get("/auctions/$id"))
                .andExpect(status().isOk)
                .andDo {
                    logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString)
                    assertThat(content).isEqualTo(it.response.contentAsString)
                }
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isString)
                .andExpect(jsonPath("$.auctionType").value("CCA_VCG"))
                .andExpect(jsonPath("$.auction.domain").exists())
                .andExpect(jsonPath("$.auction.domain.bidders").exists())
                .andExpect(jsonPath("$.auction.domain.bidders[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").value("0"))
                .andExpect(jsonPath("$.auction.domain.bidders[1].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").value("1"))
                .andExpect(jsonPath("$.auction.domain.bidders[2].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[2].name").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[2].name").value("2"))
                .andExpect(jsonPath("$.auction.domain.goods").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0]").exists())
                .andExpect(jsonPath("$.auction.domain.goods[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.goods[0].id").value("A"))
                .andExpect(jsonPath("$.auction.domain.goods[0].availability").isNumber)
                .andExpect(jsonPath("$.auction.domain.goods[0].availability").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[1]").exists())
                .andExpect(jsonPath("$.auction.domain.goods[1].id").isString)
                .andExpect(jsonPath("$.auction.domain.goods[1].id").value("B"))
                .andExpect(jsonPath("$.auction.domain.goods[1].availability").isNumber)
                .andExpect(jsonPath("$.auction.domain.goods[1].availability").value(1))
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds").isEmpty)
                .andExpect(jsonPath("$.auction.currentPrices").exists())
                .andExpect(jsonPath("$.auction.currentPrices.A").isNumber)
                .andExpect(jsonPath("$.auction.currentPrices.B").isNumber)
                .andExpect(jsonPath("$.auction.currentRoundType").value("CLOCK"))
                .andExpect(jsonPath("$.auction.supplementaryRounds").isArray)

        mvc.perform(
                post("/auctions/$id/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject()
                                .put(bidder1Id, JSONArray().put(JSONObject().put("amount", 2).put("bundle", JSONObject().put("B", 1))))
                                .put(bidder2Id, JSONArray().put(JSONObject().put("amount", 3).put("bundle", JSONObject().put("A", 1)))).toString()))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id!!))
                .andExpect(jsonPath("$.auction.rounds").isEmpty)

        mvc.perform(post("/auctions/$id/close-round"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(id!!))
                .andExpect(jsonPath("$.auction.currentRoundType").value("SUPPLEMENTARY"))
                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult").exists())
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$bidder2Id.value").value(3))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$bidder2Id.goods.A").value(1))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$bidder1Id.value").value(2))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$bidder1Id.goods.B").value(1))
                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.totalPayments").value(0))
                .andExpect(jsonPath("$.auction.rounds[0].type").value(CCARound.Type.CLOCK.name))


    }
}

