package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.mechlib.domain.Allocation
import org.marketdesignresearch.mechlib.domain.Payment
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class PaymentJsonSerialization {

    class PaymentJsonSerializer : JsonSerializer<Payment>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(payment: Payment, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeNumberField("totalPayments", payment.totalPayments)
            payment.paymentMap.forEach { jsonGenerator.writeNumberField(it.key.id, it.value.amount) }
            jsonGenerator.writeEndObject()
        }
    }

}