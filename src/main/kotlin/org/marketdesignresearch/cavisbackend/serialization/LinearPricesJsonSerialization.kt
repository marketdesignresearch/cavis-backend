package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.mechlib.auction.Auction
import org.marketdesignresearch.mechlib.domain.price.LinearPrices
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class LinearPricesJsonSerialization {

    class LinearPricesJsonSerializer : JsonSerializer<LinearPrices>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(linearPrices: LinearPrices, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            linearPrices.entrySet().forEach { jsonGenerator.writeObjectField(it.key.id, it.value.amount) }
            jsonGenerator.writeEndObject()
        }
    }

}