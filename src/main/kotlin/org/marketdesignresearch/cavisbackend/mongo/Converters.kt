package org.marketdesignresearch.cavisbackend.mongo


import org.bson.*
import org.bson.codecs.EncoderContext
import org.bson.conversions.Bson
import org.marketdesignresearch.mechlib.core.Allocation
import org.marketdesignresearch.mechlib.core.BidderAllocation
import org.marketdesignresearch.mechlib.core.BidderPayment
import org.marketdesignresearch.mechlib.core.Payment
import org.marketdesignresearch.mechlib.core.bid.Bid
import org.marketdesignresearch.mechlib.core.bid.Bids
import org.marketdesignresearch.mechlib.core.bidder.Bidder
import org.marketdesignresearch.mechlib.mechanism.auctions.cca.CCAuction
import org.marketdesignresearch.mechlib.metainfo.MetaInfo
import org.spectrumauctions.sats.core.model.SATSGood
import org.spectrumauctions.sats.core.model.gsvm.GSVMBidder
import org.spectrumauctions.sats.core.model.gsvm.GSVMLicense
import org.spectrumauctions.sats.core.model.gsvm.GSVMWorld
import org.spectrumauctions.sats.core.util.file.gson.GsonWrapper
import org.spectrumauctions.sats.core.util.instancehandling.JSONInstanceHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component
import java.util.*
import kotlin.collections.HashMap
import org.springframework.core.convert.ConversionService
import kotlin.NoSuchElementException

class GSVMLicenseToDocumentConverter : Converter<GSVMLicense, Document> {
    override fun convert(source: GSVMLicense): Document {
        val document = Document()
        document["world"] = GsonWrapper.getInstance().toJson(source.world)
        document["name"] = source.name
        return document
    }
}

class DocumentToGSVMLicenseConverter : Converter<Document, GSVMLicense> {
    override fun convert(source: Document): GSVMLicense {
        val world = GsonWrapper.getInstance().fromJson(GSVMWorld::class.java, source["world"] as String)
        val name = source["name"] as String
        return world.licenses.find { it.name == name } ?: throw NoSuchElementException("No license with name $name found.")
    }
}

class GSVMBidderToDocumentConverter : Converter<GSVMBidder, Document> {
    override fun convert(source: GSVMBidder): Document {
        val document = Document()
        document["world"] = GsonWrapper.getInstance().toJson(source.world)
        document["name"] = source.name
        return document
    }
}