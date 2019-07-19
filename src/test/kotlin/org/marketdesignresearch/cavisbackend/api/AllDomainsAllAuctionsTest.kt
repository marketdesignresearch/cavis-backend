package org.marketdesignresearch.cavisbackend.api

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
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
import java.util.stream.Stream

@SpringBootTest
@AutoConfigureMockMvc
//@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AllDomainsAllAuctionsTest {

    private val logger = LoggerFactory.getLogger(AllDomainsAllAuctionsTest::class.java)

    @Autowired
    lateinit var mvc: MockMvc

    @TestFactory
    fun `Dynamically testing all auction types in all domains`(): Stream<DynamicTest> {

        val domains: List<String> = listOf(
                "additiveValue",
                "unitDemandValue",
                "gsvm"
                //"lsvm",
                //"mrvm"
        )

        val auctionTypes: List<String> = listOf(
                "SEQUENTIAL_FIRST_PRICE",
                "SEQUENTIAL_SECOND_PRICE",
                "SIMULTANEOUS_FIRST_PRICE",
                "SIMULTANEOUS_SECOND_PRICE",
                "VCG_XOR",
                "CCA_VCG",
                "CCA_CCG",
                "PVM_VCG",
                "PVM_CCG"
        )

        return domains.stream()
                .flatMap { domain ->
                    auctionTypes.stream().map { auctionType ->
                        DynamicTest.dynamicTest("Testing $auctionType in $domain...") {
                            var id: String? = null
                            var content: String? = null
                            var bigDomain = false

                            val body = JSONObject()
                                    .put("domain", JSONObject()
                                            .put("type", domain)
                                            .put("seed", 12345L))
                                    .put("auctionType", auctionType)

                            mvc.perform(
                                    post("/auctions/")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(body.toString()))
                                    .andExpect(status().isOk)
                                    .andDo {
                                        content = it.response.contentAsString
                                        id = JSONObject(content).getString("id")
                                        val jsonDomain = JSONObject(content).getJSONObject("auction").getJSONObject("domain")
                                        try {
                                            jsonDomain.getJSONArray("bidders").getJSONObject(10)
                                            bigDomain = true
                                        } catch (e: JSONException) {
                                            // Ok, it did not exist
                                        }
                                        try {
                                            jsonDomain.getJSONArray("goods").getJSONObject(10)
                                            bigDomain = true
                                        } catch (e: JSONException) {
                                            // Ok, it did not exist
                                        }
                                    }

                            mvc.perform(get("/auctions/$id"))
                                    .andExpect(status().isOk)
                                    .andExpect(jsonPath("$.id").value(id!!))
                                    .andExpect(jsonPath("$.auctionType").value(auctionType))
                                    .andExpect(jsonPath("$.auction.rounds").isArray)
                                    .andExpect(jsonPath("$.auction.rounds").isEmpty)
                                    .andDo {
                                        logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString)
                                        assertThat(content).isEqualTo(it.response.contentAsString)
                                    }

                            if (!bigDomain) {
                                mvc.perform(post("/auctions/$id/finish"))
                                        .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                                        .andExpect(status().isOk)
                                        .andExpect(jsonPath("$.id").value(id!!))
                                        .andExpect(jsonPath("$.auction.rounds").isArray)
                                        .andExpect(jsonPath("$.auction.rounds[0]").exists())
                            } else {
                                mvc.perform(post("/auctions/$id/advance-round"))
                                        .andDo { logger.info("Request: {} | Response: {}", it.request.contentAsString, it.response.contentAsString) }
                                        .andExpect(status().isOk)
                                        .andExpect(jsonPath("$.id").value(id!!))
                                        .andExpect(jsonPath("$.auction.rounds").isArray)
                                        .andExpect(jsonPath("$.auction.rounds[0]").exists())
                            }

                        }
                    }
                }


    }
}

