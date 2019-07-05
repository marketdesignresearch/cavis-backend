package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.spectrumauctions.sats.core.model.gsvm.GSVMBidder
import org.spectrumauctions.sats.core.model.gsvm.GSVMLicense
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class GSVMBidderJsonSerialization {
    class GSVMBidderJsonSerializer : JsonSerializer<GSVMBidder>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(gsvmBidder: GSVMBidder, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("id", gsvmBidder.id.toString())
            jsonGenerator.writeStringField("name", gsvmBidder.name)
            jsonGenerator.writeStringField("description", gsvmBidder.description)
            jsonGenerator.writeNumberField("position", gsvmBidder.bidderPosition)
            jsonGenerator.writeEndObject()
        }
    }
}