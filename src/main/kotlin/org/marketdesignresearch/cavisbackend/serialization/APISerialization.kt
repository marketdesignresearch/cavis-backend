package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.TextNode
import org.marketdesignresearch.cavisbackend.sha256Hex
import org.marketdesignresearch.mechlib.auction.Auction
import org.marketdesignresearch.mechlib.auction.cca.CCARound
import org.marketdesignresearch.mechlib.auction.cca.CCAuction
import org.marketdesignresearch.mechlib.auction.pvm.PVMAuction
import org.marketdesignresearch.mechlib.auction.pvm.ml.InferredValueFunctions
import org.marketdesignresearch.mechlib.domain.*
import org.marketdesignresearch.mechlib.domain.bid.Bids
import org.marketdesignresearch.mechlib.domain.bidder.Bidder
import org.marketdesignresearch.mechlib.domain.bidder.value.Value
import org.marketdesignresearch.mechlib.domain.price.LinearPrices
import org.marketdesignresearch.mechlib.mechanisms.MechanismResult
import org.spectrumauctions.sats.core.model.gsvm.GSVMBidder
import org.spectrumauctions.sats.core.model.gsvm.GSVMLicense
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException


@JsonComponent
class APISerialization {

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

    class AuctionJsonSerializer : JsonSerializer<Auction>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(auction: Auction, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField("domain", auction.domain) // TODO: Add keyword for domain
            jsonGenerator.writeStringField("mechanismType", auction.mechanismType.mechanismName)
            jsonGenerator.writeObjectField("currentPrices", auction.currentPrices)
            jsonGenerator.writeBooleanField("finished", auction.finished())
            if (auction is CCAuction) {
                jsonGenerator.writeObjectField("supplementaryRounds", auction.supplementaryRounds)
                jsonGenerator.writeStringField("currentRoundType",
                        if (auction.currentRoundType == CCARound.Type.CLOCK) "Clock Round"
                        else "Supplementary Round")
            }
            if (auction is PVMAuction) {
                jsonGenerator.writeStringField("currentRoundType", if (auction.numberOfRounds == 0) "Initial Round" else "Elicitation Round")
            }
            jsonGenerator.writeArrayFieldStart("rounds")
            for (i in 0 until auction.numberOfRounds) {
                jsonGenerator.writeObject(auction.getRound(i))
            }
            jsonGenerator.writeEndArray()
            jsonGenerator.writeObjectFieldStart("restrictedBids")
            auction.restrictedBids().forEach{ jsonGenerator.writeObjectField(it.key.id.toString(), it.value) }
            jsonGenerator.writeEndObject()
            jsonGenerator.writeNumberField("allowedNumberOfBids", auction.allowedNumberOfBids())
            jsonGenerator.writeEndObject()
        }
    }

    class DomainJsonSerializer : JsonSerializer<Domain>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(domain: Domain, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField("type", domain::class.simpleName)
            jsonGenerator.writeObjectField("bidders", domain.bidders)
            jsonGenerator.writeObjectField("goods", domain.goods)
            if (domain.hasEfficientAllocationCalculated()) {
                jsonGenerator.writeObjectField("efficientAllocation", domain.efficientAllocation)
            }
            jsonGenerator.writeEndObject()
        }
    }

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

    class BidsJsonSerializer : JsonSerializer<Bids>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(bids: Bids, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartArray()
            for ((bidder, bid) in bids.bidMap) {
                for (bundleBid in bid.bundleBids) {
                    jsonGenerator.writeStartObject()
                    //jsonGenerator.writeStringField("id", bundleBid.id)
                    jsonGenerator.writeNumberField("amount", bundleBid.amount)
                    jsonGenerator.writeNumberField("value", bidder.getValue(bundleBid.bundle))
                    jsonGenerator.writeObjectField("bundle", bundleBid.bundle)
                    jsonGenerator.writeStringField("bidderId", bidder.id.toString())
                    jsonGenerator.writeEndObject()
                }
            }
            jsonGenerator.writeEndArray()
        }
    }

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

    class GSVMLicenseJsonSerializer : JsonSerializer<GSVMLicense>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(gsvmLicense: GSVMLicense, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("id", gsvmLicense.uuid.toString())
            jsonGenerator.writeStringField("name", gsvmLicense.name)
            jsonGenerator.writeNumberField("longId", gsvmLicense.longId)
            jsonGenerator.writeNumberField("position", gsvmLicense.position)
            jsonGenerator.writeStringField("circle", if (gsvmLicense.world.nationalCircle.licenses.contains(gsvmLicense)) "national" else "regional")
            jsonGenerator.writeNumberField("availability", gsvmLicense.available())
            jsonGenerator.writeEndObject()
        }
    }

    class LinearPricesJsonSerializer : JsonSerializer<LinearPrices>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(linearPrices: LinearPrices, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            linearPrices.entrySet().forEach { jsonGenerator.writeObjectField(it.key.uuid.toString(), it.value.amount) }
            jsonGenerator.writeEndObject()
        }
    }

    class PaymentJsonSerializer : JsonSerializer<Payment>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(payment: Payment, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeNumberField("totalPayments", payment.totalPayments)
            payment.paymentMap.forEach { jsonGenerator.writeNumberField(it.key.id.toString(), it.value.amount) }
            jsonGenerator.writeEndObject()
        }
    }

    class GoodJsonSerializer : JsonSerializer<Good>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(good: Good, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("name", good.name)
            jsonGenerator.writeStringField("id", good.uuid.toString())
            jsonGenerator.writeNumberField("availability", good.available())
            jsonGenerator.writeEndObject()
        }
    }

    class SimpleGoodJsonDeserializer : JsonDeserializer<SimpleGood>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun deserialize(jsonParser: JsonParser,
                                 deserializationContext: DeserializationContext): SimpleGood {

            val treeNode: TreeNode = jsonParser.codec.readTree(jsonParser)
            val name = treeNode.get("name") as TextNode
            val dummyGood = treeNode.get("dummyGood") as? BooleanNode
            val availability = treeNode.get("availability") as? IntNode
            return SimpleGood(name.asText(), availability?.asInt() ?: 1, dummyGood?.asBoolean() ?: false)
        }
    }

    class InferredValueFunctionsSerializer : JsonSerializer<InferredValueFunctions>() {
        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(valueFunctions: InferredValueFunctions, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            valueFunctions.forEach { jsonGenerator.writeObjectField(it.key.id.toString(), it.value) }
            jsonGenerator.writeEndObject()
        }
    }

}