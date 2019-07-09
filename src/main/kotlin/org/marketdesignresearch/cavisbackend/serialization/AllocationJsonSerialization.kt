package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.mechlib.domain.Allocation
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class AllocationJsonSerialization {

    class AllocationJsonSerializer : JsonSerializer<Allocation>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(allocation: Allocation, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            allocation.tradesMap.forEach {
                jsonGenerator.writeObjectFieldStart(it.key.id.toString())
                jsonGenerator.writeNumberField("value", it.value.value)
                jsonGenerator.writeObjectField("bundle", it.value.bundle)
                jsonGenerator.writeEndObject()
            }
            jsonGenerator.writeEndObject()
        }
    }

}