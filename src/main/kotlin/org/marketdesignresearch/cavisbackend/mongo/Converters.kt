package org.marketdesignresearch.cavisbackend.mongo


import com.google.common.collect.ImmutableMap
import org.bson.Document
import org.spectrumauctions.sats.core.model.GenericWorld
import org.spectrumauctions.sats.core.model.SATSGood
import org.spectrumauctions.sats.core.model.World
import org.spectrumauctions.sats.core.model.gsvm.GSVMBidder
import org.spectrumauctions.sats.core.util.file.gson.GsonWrapper
import org.springframework.core.convert.converter.Converter

class SATSGoodToDocumentConverter : Converter<SATSGood, Document> {
    override fun convert(source: SATSGood): Document {
        val document = Document()
        document["world"] = GsonWrapper.getInstance().toJson(source.world)
        document["name"] = source.name
        return document
    }
}

class DocumentToSATSGoodConverter : Converter<Document, SATSGood> {
    override fun convert(source: Document): SATSGood {
        val world = GsonWrapper.getInstance().fromJson(World::class.java, source["world"] as String)
        val name = source["name"] as String
        return world.licenses.find { it.name == name } ?: run {
            if (world is GenericWorld) world.allGenericDefinitions.find { it.name == name } ?: throw NoSuchElementException("No good with name $name found.")
            else throw NoSuchElementException("No good with name $name found.")
        }
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