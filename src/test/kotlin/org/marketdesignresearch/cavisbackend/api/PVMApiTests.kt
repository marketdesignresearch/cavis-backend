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
import org.assertj.core.api.Assertions.*

@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PVMApiTests {

    private val logger = LoggerFactory.getLogger(PVMApiTests::class.java)

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
                            .put(JSONObject().put("name", "B"))
                            .put(JSONObject().put("name", "C"))
                            .put(JSONObject().put("name", "D"))))
            .put("auctionType", "PVM_VCG")

    @Test
    fun `Should create new PVM auction`() {

        var id: String? = null
        var content: String? = null
        var bidder1Id: String? = null
        var bidder2Id: String? = null
        var bidder3Id: String? = null
        var item1Id: String? = null
        var item2Id: String? = null
        var item3Id: String? = null
        var item4Id: String? = null

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
                    item3Id = goodsArray.getJSONObject(2).getString("id")
                    item4Id = goodsArray.getJSONObject(3).getString("id")
                }

        mvc.perform(get("/auctions/$id"))
                .andExpect(status().isOk)
                .andDo {
                    logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString)
                    assertThat(content).isEqualTo(it.response.contentAsString)
                }
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.id").isString)
                .andExpect(jsonPath("$.auctionType").value("PVM_VCG"))
                .andExpect(jsonPath("$.auction.domain").exists())
                .andExpect(jsonPath("$.auction.domain.bidders").exists())
                .andExpect(jsonPath("$.auction.domain.bidders[0].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[0].name").value("1"))
                .andExpect(jsonPath("$.auction.domain.bidders[1].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[1].name").value("2"))
                .andExpect(jsonPath("$.auction.domain.bidders[2].id").isString)
                .andExpect(jsonPath("$.auction.domain.bidders[2].name").isString)
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
                .andExpect(jsonPath("$.auction.domain.goods[2].id").value(item3Id!!))
                .andExpect(jsonPath("$.auction.domain.goods[2].name").value("C"))
                .andExpect(jsonPath("$.auction.domain.goods[2].quantity").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[3].id").value(item4Id!!))
                .andExpect(jsonPath("$.auction.domain.goods[3].name").value("D"))
                .andExpect(jsonPath("$.auction.domain.goods[3].quantity").value(1))
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds").isEmpty)
                .andExpect(jsonPath("$.auction.restrictedBids").exists())
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder1Id").doesNotExist())
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder2Id").doesNotExist())
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder3Id").doesNotExist())
                .andExpect(jsonPath("$.auction.allowedNumberOfBids").value(5))

        mvc.perform(post("/auctions/$id/advance-round"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.auction.rounds[0]").exists())
                .andExpect(jsonPath("$.auction.rounds[0].inferredValues").exists())
                .andExpect(jsonPath("$.auction.rounds[1]").doesNotExist())
                .andExpect(jsonPath("$.auction.restrictedBids").exists())
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder1Id").isArray)
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder2Id").isArray)
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder3Id").isArray)
                .andExpect(jsonPath("$.auction.allowedNumberOfBids").value(1))

        mvc.perform(
                post("/auctions/$id/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONObject()
                                .put(bidder1Id, JSONArray().put(JSONObject().put("amount", 2).put("bundle", JSONObject().put(item2Id, 1))))
                                .put(bidder2Id, JSONArray().put(JSONObject().put("amount", 3).put("bundle", JSONObject().put(item1Id, 1)))).toString()))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isBadRequest)

        mvc.perform(post("/auctions/$id/advance-round"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.auction.rounds[0]").exists())
                .andExpect(jsonPath("$.auction.rounds[0].inferredValues.$bidder1Id").exists())
                .andExpect(jsonPath("$.auction.rounds[0].inferredValues.$bidder2Id").exists())
                .andExpect(jsonPath("$.auction.rounds[0].inferredValues.$bidder3Id").exists())
                .andExpect(jsonPath("$.auction.rounds[1]").exists())
                .andExpect(jsonPath("$.auction.rounds[1].inferredValues.$bidder1Id").exists())
                .andExpect(jsonPath("$.auction.rounds[1].inferredValues.$bidder2Id").exists())
                .andExpect(jsonPath("$.auction.rounds[1].inferredValues.$bidder3Id").exists())
                .andExpect(jsonPath("$.auction.rounds[2]").doesNotExist())
                .andExpect(jsonPath("$.auction.restrictedBids").exists())
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder1Id").isArray)
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder2Id").isArray)
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder3Id").isArray)
                .andExpect(jsonPath("$.auction.allowedNumberOfBids").value(1))

        val bids = JSONArray(mvc.perform(post("/auctions/$id/propose"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andReturn().response.contentAsString)

        // Advance, to check if the proposed bids were the actually applied bids
        val nextRoundBids = JSONObject(mvc.perform(post("/auctions/$id/advance-round"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.auction.rounds[0]").exists())
                .andExpect(jsonPath("$.auction.rounds[1]").exists())
                .andExpect(jsonPath("$.auction.rounds[2]").exists())
                .andExpect(jsonPath("$.auction.rounds[3]").doesNotExist())
                .andExpect(jsonPath("$.auction.restrictedBids").exists())
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder1Id").isArray)
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder2Id").isArray)
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder3Id").isArray)
                .andExpect(jsonPath("$.auction.allowedNumberOfBids").value(1))
                .andReturn().response.contentAsString).getJSONObject("auction").getJSONArray("rounds").getJSONObject(2).getJSONArray("bids")

        assertThat(bids.toString()).isEqualTo(nextRoundBids.toString())

    }

    @Test
    fun `Should create new PVM auction and jump to end`() {

        var id: String? = null

        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk)
                .andDo { id = JSONObject(it.response.contentAsString).getString("id") }

        mvc.perform(post("/auctions/$id/finish"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)

    }

}

