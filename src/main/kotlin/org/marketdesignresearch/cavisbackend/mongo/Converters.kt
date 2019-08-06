package org.marketdesignresearch.cavisbackend.mongo


import org.bson.Document
import org.marketdesignresearch.mechlib.core.Allocation
import org.marketdesignresearch.mechlib.core.BidderAllocation
import org.marketdesignresearch.mechlib.core.BidderPayment
import org.marketdesignresearch.mechlib.core.Payment
import org.marketdesignresearch.mechlib.core.bid.Bid
import org.marketdesignresearch.mechlib.core.bid.Bids
import org.marketdesignresearch.mechlib.core.bidder.Bidder
import org.marketdesignresearch.mechlib.metainfo.MetaInfo
import org.springframework.core.convert.converter.Converter
import java.util.*
import kotlin.collections.HashMap

class AllocationConverter : Converter<Allocation, Document> {
    override fun convert(source: Allocation): Document {
        val document = Document()
        document["totalAllocationValue"] = source.totalAllocationValue
        document["bids"] = BidsConverter().convert(source.bids)
        document["metaInfo"] = MetaInfoConverter().convert(source.metaInfo)
        document["coalitions"] = source.potentialCoalitions
        val bidderMap = HashMap<String, Bidder>()
        val tradesMap = HashMap<String, BidderAllocation>()
        source.tradesMap.forEach { (bidder, bidderAllocation) ->
            bidderMap[bidder.id.toString()] = bidder
            tradesMap[bidder.id.toString()] = bidderAllocation
        }
        document["bidderMap"] = bidderMap
        document["tradesMap"] = tradesMap
        return document
    }
}

class PaymentConverter : Converter<Payment, Document> {
    override fun convert(source: Payment): Document {
        val document = Document()
        document["metaInfo"] = MetaInfoConverter().convert(source.metaInfo)
        val bidderMap = HashMap<String, Bidder>()
        val tradesMap = HashMap<String, BidderPayment>()
        source.paymentMap.forEach { (bidder, bidderPayment) ->
            bidderMap[bidder.id.toString()] = bidder
            tradesMap[bidder.id.toString()] = bidderPayment
        }
        document["bidderMap"] = bidderMap
        document["paymentMap"] = tradesMap
        return document
    }
}

class BidsConverter : Converter<Bids, Document> {
    override fun convert(source: Bids): Document {
        val document = Document()
        val bidderMap = HashMap<String, Bidder>()
        val bidMap = HashMap<String, Bid>()
        source.bidMap.forEach { (bidder, bid) ->
            bidderMap[bidder.id.toString()] = bidder // FIXME: Don't assign directly, let it convert by registry
            bidMap[bidder.id.toString()] = bid
        }
        document["bidderMap"] = bidderMap
        document["bidMap"] = bidMap
        return document
    }
}

class MetaInfoConverter : Converter<MetaInfo, Document> {
    override fun convert(source: MetaInfo): Document {
        val document = Document()
        document["javaRuntime"] = source.javaRuntime
        document["mipSolveTime"] = source.mipSolveTime
        document["lpSolveTime"] = source.lpSolveTime
        document["qpSolveTime"] = source.qpSolveTime
        document["approxSolveTime"] = source.approxSolveTime
        document["numberOfLPs"] = source.numberOfLPs
        document["numberOfQPs"] = source.numberOfQPs
        document["numberOfApproximations"] = source.numberOfApproximations
        document["numberOfMIPs"] = source.numberOfMIPs
        document["constraintsGenerated"] = source.constraintsGenerated
        document["ignoredConstraints"] = source.ignoredConstraints
        return document
    }
}

