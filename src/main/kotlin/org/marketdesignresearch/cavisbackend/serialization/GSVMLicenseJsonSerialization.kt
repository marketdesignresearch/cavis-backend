package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.spectrumauctions.sats.core.model.gsvm.GSVMLicense
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class GSVMLicenseJsonSerialization {
    class GSVMLicenseJsonSerializer : JsonSerializer<GSVMLicense>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(gsvmLicense: GSVMLicense, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("id", gsvmLicense.id)
            jsonGenerator.writeNumberField("position", gsvmLicense.position)
            jsonGenerator.writeStringField("circle", if (gsvmLicense.world.nationalCircle.licenses.contains(gsvmLicense)) "national" else "regional")
            jsonGenerator.writeNumberField("availability", gsvmLicense.available())
            jsonGenerator.writeEndObject()
        }
    }
}