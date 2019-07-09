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
import org.marketdesignresearch.cavisbackend.server.JSONBid
import org.marketdesignresearch.mechlib.auction.cca.CCARound
import java.util.*
import kotlin.collections.HashMap

@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PVMApiTests {

    private val logger = LoggerFactory.getLogger(PVMApiTests::class.java)

    @Autowired
    lateinit var mvc: MockMvc

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

        val body = JSONObject()
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
                .andExpect(jsonPath("$.auction.domain.goods[0].availability").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[0].dummyGood").value(false))
                .andExpect(jsonPath("$.auction.domain.goods[1]").exists())
                .andExpect(jsonPath("$.auction.domain.goods[1].id").value(item2Id!!))
                .andExpect(jsonPath("$.auction.domain.goods[1].name").value("B"))
                .andExpect(jsonPath("$.auction.domain.goods[1].availability").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[1].dummyGood").value(false))
                .andExpect(jsonPath("$.auction.domain.goods[2].id").value(item3Id!!))
                .andExpect(jsonPath("$.auction.domain.goods[2].name").value("C"))
                .andExpect(jsonPath("$.auction.domain.goods[2].availability").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[2].dummyGood").value(false))
                .andExpect(jsonPath("$.auction.domain.goods[3].id").value(item4Id!!))
                .andExpect(jsonPath("$.auction.domain.goods[3].name").value("D"))
                .andExpect(jsonPath("$.auction.domain.goods[3].availability").value(1))
                .andExpect(jsonPath("$.auction.domain.goods[3].dummyGood").value(false))
                .andExpect(jsonPath("$.auction.rounds").isArray)
                .andExpect(jsonPath("$.auction.rounds").isEmpty)
                .andExpect(jsonPath("$.auction.restrictedBids").exists())
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder1Id").doesNotExist())
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder2Id").doesNotExist())
                .andExpect(jsonPath("$.auction.restrictedBids.$bidder3Id").doesNotExist())
                .andExpect(jsonPath("$.auction.allowedNumberOfBids").value(5))

        mvc.perform(post("/auctions/$id/advance"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.auction.rounds[0]").exists())
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

        mvc.perform(post("/auctions/$id/advance"))
                .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.auction.rounds[0]").exists())
                .andExpect(jsonPath("$.auction.rounds[1]").exists())
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
        val nextRoundBids = JSONObject(mvc.perform(post("/auctions/$id/advance"))
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

        // TODO: Place valid bids, close auction

    }

//    @Test
//    fun `Should fail to create new auction`() {
//
//        // Bad media type
//        mvc.perform(
//                post("/auctions/")
//                        .contentType(MediaType.TEXT_PLAIN)
//                        .content(body().toString()))
//                .andExpect(status().isUnsupportedMediaType)
//
//        // Auction type missing
//        mvc.perform(
//                post("/auctions/")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(body().remove("auctionType").toString()))
//                .andExpect(status().isBadRequest)
//
//        // DomainWrapper type missing
//        mvc.perform(
//                post("/auctions/")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(body().put("domain", body().getJSONObject("domain").remove("type")).toString()))
//                .andExpect(status().isBadRequest)
//
//    }
//
//    @Test
//    fun `Should get auctions`() {
//        val created1 = created()
//        val id1 = created1.getString("id")
//
//        val created2 = created()
//        val id2 = created2.getString("id")
//
//        mvc.perform(get("/auctions"))
//                .andExpect(status().isOk)
//                .andExpect(jsonPath("$").isArray)
//                .andDo { result -> logger.info(result.response.contentAsString) }
//    }
//
//    @Test
//    fun `Should place bids`() {
//        val created = created()
//        val id = created.getString("id")
//        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id");
//        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id");
//
//        mvc.perform(
//                post("/auctions/$id/bids")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(bids(bidder1Uuid, bidder2Uuid).toString()))
//                .andExpect(status().isOk)
//                .andExpect(jsonPath("$.id").value(id))
//                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
//                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult").exists())
//                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$bidder2Uuid.value").value(12))
//                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.allocation.$bidder2Uuid.goods.item").value(1))
//                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.totalPayments").value(10))
//                .andExpect(jsonPath("$.auction.rounds[0].mechanismResult.payments.$bidder2Uuid").value(10))
//                .andDo { result -> logger.info(result.response.contentAsString) }
//    }
//
//    @Test
//    fun `Should reset auction`() {
//        val created = created()
//        val id = created.getString("id")
//        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id");
//        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id");
//
//        for (i in 0..4) {
//            mvc.perform(
//                    post("/auctions/$id/bids")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(bids(bidder1Uuid, bidder2Uuid).toString()))
//                    .andExpect(status().isOk)
//        }
//
//        mvc.perform(get("/auctions/$id/"))
//                .andExpect(status().isOk)
//                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
//                .andExpect(jsonPath("$.auction.rounds").isArray)
//                .andExpect(jsonPath("$.auction.rounds[0].bids").exists())
//                .andExpect(jsonPath("$.auction.rounds[1].bids").exists())
//                .andExpect(jsonPath("$.auction.rounds[2].bids").exists())
//                .andExpect(jsonPath("$.auction.rounds[3].bids").exists())
//                .andExpect(jsonPath("$.auction.rounds[4].bids").exists())
//
//        mvc.perform(
//                put("/auctions/$id/reset")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"round\": 3}"))
//                .andExpect(status().isOk)
//                .andExpect(jsonPath("$.auction.rounds").isNotEmpty)
//                .andExpect(jsonPath("$.auction.rounds").isArray)
//                .andExpect(jsonPath("$.auction.rounds[0].bids").exists())
//                .andExpect(jsonPath("$.auction.rounds[1].bids").exists())
//                .andExpect(jsonPath("$.auction.rounds[2].bids").exists())
//                .andExpect(jsonPath("$.auction.rounds[3].bids").doesNotExist())
//                .andExpect(jsonPath("$.auction.rounds[4].bids").doesNotExist())
//
//
//    }
//
//    @Test
//    fun `Should get result`() {
//        mvc.perform(get("/auctions/${finished()}/result"))
//                .andExpect(status().isOk)
//                //.andExpect(jsonPath("$.allocation.B.value").value(12))
//                //.andExpect(jsonPath("$.allocation.B.goods.item").value(1))
//                //.andExpect(jsonPath("$.payments.B").value(10))
//                .andExpect(jsonPath("$.payments.totalPayments").value(10))
//                .andDo { result -> logger.info(result.response.contentAsString) }
//    }
//
//    private fun body(): JSONObject = JSONObject()
//            .put("domain", JSONObject()
//                    .put("type", "unitDemandValue")
//                    .put("bidders", JSONArray()
//                            .put(JSONObject()
//                                    .put("name", "A"))
//                            .put(JSONObject()
//                                    .put("name", "B")))
//                    .put("goods", JSONArray().put(JSONObject().put("id", "item"))))
//            .put("auctionType", "SINGLE_ITEM_SECOND_PRICE")
//
//    private fun created(): JSONObject = JSONObject(mvc.perform(
//            post("/auctions/")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .content(body().toString()))
//            .andReturn().response.contentAsString)
//
//    private fun bids(bidder1Uuid: String, bidder2Uuid: String): JSONObject = JSONObject()
//            .put(bidder1Uuid, JSONArray().put(JSONObject().put("amount", 10).put("bundle", JSONObject().put("item", 1))))
//            .put(bidder2Uuid, JSONArray().put(JSONObject().put("amount", 12).put("bundle", JSONObject().put("item", 1))))
//
//    private fun finished(): String {
//        val created = created()
//        val id = created.getString("id")
//        val bidder1Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(0).getString("id");
//        val bidder2Uuid = created.getJSONObject("auction").getJSONObject("domain").getJSONArray("bidders").getJSONObject(1).getString("id");
//
//        mvc.perform(
//                post("/auctions/$id/bids")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(bids(bidder1Uuid, bidder2Uuid).toString()))
//                .andExpect(status().isOk)
//        return id
//    }
}

