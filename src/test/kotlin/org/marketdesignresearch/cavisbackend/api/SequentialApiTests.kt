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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@AutoConfigureMockMvc
class SequentialApiTests {

    private val logger = LoggerFactory.getLogger(SequentialApiTests::class.java)

    @Autowired
    lateinit var wac: WebApplicationContext

    lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build()
    }

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
            .put("auctionType", "SEQUENTIAL_SECOND_PRICE")

    @Test
    fun `Should use value query correctly in new Sequential Auction`() {

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

        mvc.perform(post("/auctions/$id/advance-round"))
                .andDo { logger.info("Response: {}", it.response.contentAsString) }
                .andExpect(status().isOk)

        val valueQueryContent = JSONObject()
                .put("bundles", JSONArray()
                        .put(JSONObject().put(item1Id, 1)))

        mvc.perform(
                post("/auctions/$id/valuequery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(valueQueryContent.toString()))
                .andDo { result -> logger.info("Response: {}", result.response.contentAsString) }
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.$bidder1Id").isArray)
                .andExpect(jsonPath("$.$bidder1Id[0].value").isNumber)
                .andExpect(jsonPath("$.$bidder1Id[0].bundle.hash").isString)
                .andExpect(jsonPath("$.$bidder1Id[0].bundle.entries[0].good").isString)
                .andExpect(jsonPath("$.$bidder1Id[0].bundle.entries[0].amount").isNumber)
                .andExpect(jsonPath("$.$bidder1Id[0].bundle.entries[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder1Id[1]").doesNotExist())
                .andExpect(jsonPath("$.$bidder2Id").isArray)
                .andExpect(jsonPath("$.$bidder3Id").isArray)
    }
}

