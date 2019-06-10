package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.mechlib.mechanisms.MechanismResult
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class AuctionResultJsonSerialization {

    class AuctionResultJsonSerializer : JsonSerializer<MechanismResult>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(auctionResult: MechanismResult, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField("allocation", auctionResult.allocation)
            jsonGenerator.writeObjectField("payments", auctionResult.payment)
            jsonGenerator.writeEndObject()
        }
    }

}