package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.marketdesignresearch.cavisbackend.management.AuctionWrapper
import org.marketdesignresearch.mechlib.domain.auction.Auction
import org.marketdesignresearch.mechlib.domain.bid.Bids
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class BidsJsonSerialization {

    class BidsJsonSerializer : JsonSerializer<Bids>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(bids: Bids, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartArray()
            for ((bidder, bid) in bids.bidMap) {
                for (bundleBid in bid.bundleBids) {
                    jsonGenerator.writeStartObject()
                    jsonGenerator.writeStringField("id", bundleBid.id)
                    jsonGenerator.writeNumberField("amount", bundleBid.amount)
                    jsonGenerator.writeObjectField("bundle", bundleBid.bundle)
                    jsonGenerator.writeStringField("bidderId", bidder.id.toString())
                    jsonGenerator.writeEndObject()
                }
            }
            jsonGenerator.writeEndArray()
        }
    }

}