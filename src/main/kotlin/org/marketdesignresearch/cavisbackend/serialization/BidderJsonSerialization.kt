package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.mechlib.domain.bidder.Bidder
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class BidderJsonSerialization {

    class BidderJsonSerializer : JsonSerializer<Bidder>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(bidder: Bidder, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("id", bidder.id.toString())
            jsonGenerator.writeStringField("name", bidder.name)
            jsonGenerator.writeStringField("description", bidder.description)
            jsonGenerator.writeEndObject()
        }
    }

}