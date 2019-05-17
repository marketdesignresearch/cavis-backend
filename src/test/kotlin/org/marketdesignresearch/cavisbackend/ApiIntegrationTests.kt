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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@RunWith(SpringRunner::class)
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTests {

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    @Throws(Exception::class)
    fun unknownUuidShoudReturn404() {
        this.mvc.perform(get("/auctions/ded9ac81-bc30-4d3b-81a7-946bde6aa7de"))
                .andExpect(status().isNotFound)
    }

    @Test
    @Throws(Exception::class)
    fun shouldCreateNewAuction() {
        val json = JSONObject()
                .put("setting", JSONObject()
                        .put("type", "simple")
                        .put("bidders", JSONArray().put(JSONObject().put("id", "A")).put(JSONObject().put("id", "B")))
                        .put("goods", JSONArray().put(JSONObject().put("id", "item"))))
                .put("type", "SINGLE_ITEM_SECOND_PRICE")

        this.mvc.perform(
                post("/auctions/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.toString()))
                .andExpect(status().isOk)
                .andExpect(MockMvcResultMatchers.jsonPath("$.uuid").exists())
                .andExpect(MockMvcResultMatchers.jsonPath("$.auction").exists())
    }
}