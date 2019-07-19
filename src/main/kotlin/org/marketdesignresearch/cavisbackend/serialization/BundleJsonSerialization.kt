package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.cavisbackend.sha256Hex
import org.marketdesignresearch.mechlib.domain.Bundle
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class BundleJsonSerialization {

    class BundleJsonSerializer : JsonSerializer<Bundle>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(bundle: Bundle, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("hash", bundle.sha256Hex())
            jsonGenerator.writeArrayFieldStart("entries")
            for (entry in bundle.bundleEntries) {
                jsonGenerator.writeStartObject()
                jsonGenerator.writeStringField("good", entry.good.uuid.toString())
                jsonGenerator.writeNumberField("amount", entry.amount)
                jsonGenerator.writeEndObject()
            }
            jsonGenerator.writeEndArray()
            jsonGenerator.writeEndObject()
        }
    }

}