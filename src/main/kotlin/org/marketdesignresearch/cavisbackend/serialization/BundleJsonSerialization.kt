package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.cavisbackend.management.AuctionWrapper
import org.marketdesignresearch.mechlib.domain.Bundle
import org.marketdesignresearch.mechlib.domain.auction.Auction
import org.marketdesignresearch.mechlib.domain.bid.Bids
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class BundleJsonSerialization {

    class BundleJsonSerializer : JsonSerializer<Bundle>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(bundle: Bundle, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartArray()
            for (entry in bundle.bundleEntries) {
                jsonGenerator.writeStartObject()
                jsonGenerator.writeObjectField("good", entry.good.id)
                jsonGenerator.writeNumberField("amount", entry.amount)
                jsonGenerator.writeEndObject()
           }
            jsonGenerator.writeEndArray()
        }
    }

}