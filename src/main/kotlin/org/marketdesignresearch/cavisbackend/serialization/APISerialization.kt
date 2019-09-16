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
import org.marketdesignresearch.mechlib.mechanism.auctions.Auction
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCAuction
import org.marketdesignresearch.mechlib.mechanism.auctions.pvm.PVMAuction
import org.marketdesignresearch.mechlib.mechanism.auctions.pvm.ml.InferredValueFunctions
import org.marketdesignresearch.mechlib.core.*
import org.marketdesignresearch.mechlib.core.bid.Bids
import org.marketdesignresearch.mechlib.core.bidder.Bidder
import org.marketdesignresearch.mechlib.core.price.LinearPrices
import org.marketdesignresearch.mechlib.core.Outcome
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCAClockRound
import org.spectrumauctions.sats.core.model.gsvm.GSVMBidder
import org.spectrumauctions.sats.core.model.gsvm.GSVMLicense
import org.spectrumauctions.sats.core.model.lsvm.LSVMBidder
import org.spectrumauctions.sats.core.model.lsvm.LSVMLicense
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
                jsonGenerator.writeNumberField("trueValue", it.key.getValue(it.value.bundle))
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
            jsonGenerator.writeObjectField("domain", auction.domain)
            jsonGenerator.writeStringField("outcomeRule", auction.outcomeRuleGenerator.name)
            jsonGenerator.writeObjectField("currentPrices", auction.currentPrices)
            jsonGenerator.writeBooleanField("finished", auction.finished())
            if (auction is CCAuction) {
                jsonGenerator.writeObjectField("supplementaryRounds", auction.supplementaryRounds)
                jsonGenerator.writeStringField("currentRoundType", // TODO
                        if (auction.currentRoundType == "CLOCK") "Clock Round"
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

    class CCAClockRoundJsonSerializer : JsonSerializer<CCAClockRound>() {
        override fun serialize(round: CCAClockRound, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {
            jsonGenerator.writeStartObject()
            jsonGenerator.writeNumberField("roundNumber", round.roundNumber)
            jsonGenerator.writeObjectField("bids", round.bids)
            jsonGenerator.writeObjectField("prices", round.prices)
            jsonGenerator.writeObjectField("outcome", round.outcome)
            jsonGenerator.writeStringField("description", round.description)
            jsonGenerator.writeStringField("type", round.type)
            jsonGenerator.writeObjectField("overDemand", round.overDemand)
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
                jsonGenerator.writeNumberField("efficientSocialWelfare", domain.efficientAllocation.trueSocialWelfare)
            }
            jsonGenerator.writeEndObject()
        }
    }

    class OutcomeJsonSerializer : JsonSerializer<Outcome>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(outcome: Outcome, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeObjectField("allocation", outcome.allocation)
            jsonGenerator.writeObjectField("payments", outcome.payment)
            jsonGenerator.writeObjectField("revenue", outcome.revenue)
            jsonGenerator.writeObjectField("socialWelfare", outcome.socialWelfare)
            jsonGenerator.writeObjectFieldStart("winnerUtilities")
            outcome.winners.forEach {
                jsonGenerator.writeNumberField(it.id.toString(), outcome.payoffOf(it))
            }
            jsonGenerator.writeEndObject()
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
            jsonGenerator.writeStringField("shortDescription", bidder.shortDescription)
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
            jsonGenerator.writeStringField("shortDescription", gsvmBidder.shortDescription)
            jsonGenerator.writeNumberField("position", gsvmBidder.bidderPosition)
            jsonGenerator.writeEndObject()
        }
    }

    class LSVMLicenseJsonSerializer : JsonSerializer<LSVMLicense>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(lsvmLicense: LSVMLicense, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("id", lsvmLicense.uuid.toString())
            jsonGenerator.writeStringField("name", lsvmLicense.name)
            jsonGenerator.writeNumberField("longId", lsvmLicense.longId)
            jsonGenerator.writeNumberField("rowPosition", lsvmLicense.rowPosition)
            jsonGenerator.writeNumberField("columnPosition", lsvmLicense.columnPosition)
            jsonGenerator.writeNumberField("quantity", lsvmLicense.quantity)
            jsonGenerator.writeEndObject()
        }
    }
    
    class LSVMBidderJsonSerializer : JsonSerializer<LSVMBidder>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(lsvmBidder: LSVMBidder, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("id", lsvmBidder.id.toString())
            jsonGenerator.writeStringField("name", lsvmBidder.name)
            jsonGenerator.writeStringField("description", lsvmBidder.description)
            jsonGenerator.writeStringField("shortDescription", lsvmBidder.shortDescription)
            jsonGenerator.writeArrayFieldStart("proximity")
            lsvmBidder.proximity.forEach { jsonGenerator.writeString(it.uuid.toString()) }
            jsonGenerator.writeEndArray()
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
            jsonGenerator.writeNumberField("quantity", gsvmLicense.quantity)
            jsonGenerator.writeEndObject()
        }
    }

    class LinearPricesJsonSerializer : JsonSerializer<LinearPrices>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(linearPrices: LinearPrices, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            linearPrices.priceMap.forEach { jsonGenerator.writeObjectField(it.key.uuid.toString(), it.value.amount) }
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
            jsonGenerator.writeNumberField("quantity", good.quantity)
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
            valueFunctions.map.forEach { jsonGenerator.writeObjectField(it.key.toString(), it.value) }
            jsonGenerator.writeEndObject()
        }
    }

}