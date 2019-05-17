package org.marketdesignresearch.cavisbackend

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class ApiTests {

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun unknownUuidShoudReturn404() {
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
                .andExpect(content().json(
                        "{" +
                        "    \"auction\": {" +
                        "        \"domain\": {" +
                        "            \"bidders\": [" +
                        "                {\"id\": \"A\"}," +
                        "                {\"id\": \"B\"}" +
                        "            ]," +
                        "            \"goods\": [" +
                        "                {\"id\": \"item\",\"availability\": 1,\"dummyGood\": false}" +
                        "            ]" +
                        "        }," +
                        "        \"mechanismType\": \"SINGLE_ITEM_SECOND_PRICE\"" +
                        "    }" +
                        "}"))
                .andExpect(jsonPath("$.auction").exists())
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

    }

    @Test
    fun shouldFailToCreateNewAuction() {

        // Bad media type
        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body().toString()))
                .andExpect(status().isUnsupportedMediaType)

        // Type missing
        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().remove("type").toString()))
                .andExpect(status().isBadRequest)

        // Setting type missing
        mvc.perform(
                post("/auctions/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body().put("setting", body().getJSONObject("setting").remove("type")).toString()))
                .andExpect(status().isBadRequest)

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
                .andExpect(jsonPath("$.allocation").exists())
    }

    @Test
    fun shouldGetResult() {
        mvc.perform(get("/auctions/${finished()}/result"))
                .andExpect(status().isOk)
    }

    private fun body(): JSONObject = JSONObject()
                .put("setting", JSONObject()
                        .put("type", "simple")
                        .put("bidders", JSONArray().put(JSONObject().put("id", "A")).put(JSONObject().put("id", "B")))
                        .put("goods", JSONArray().put(JSONObject().put("id", "item"))))
                .put("type", "SINGLE_ITEM_SECOND_PRICE")

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

