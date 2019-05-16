package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.mechlib.domain.Allocation
import org.marketdesignresearch.mechlib.mechanisms.AuctionResult
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class AuctionResultJsonSerialization {

    class AuctionResultJsonSerializer : JsonSerializer<AuctionResult>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(auctionResult: AuctionResult, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField("allocation", auctionResult.allocation)
            jsonGenerator.writeObjectField("payments", auctionResult.payment)
            jsonGenerator.writeEndObject()
        }
    }

}