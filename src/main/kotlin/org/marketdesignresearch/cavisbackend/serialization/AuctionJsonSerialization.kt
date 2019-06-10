package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.mechlib.auction.Auction
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class AuctionJsonSerialization {

    class AuctionJsonSerializer : JsonSerializer<Auction>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(auction: Auction, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField("domain", auction.domain) // TODO: Add keyword for domain
            jsonGenerator.writeStringField("mechanismType", auction.mechanismType.mechanismName)
            jsonGenerator.writeArrayFieldStart("rounds")
            for (i in 0 until auction.rounds) {
                jsonGenerator.writeObject(auction.getRound(i))
            }
            jsonGenerator.writeEndArray()
            jsonGenerator.writeEndObject()
        }
    }

}