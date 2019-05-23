package org.marketdesignresearch.cavisbackend.serialization

import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.core.JsonProcessingException
import java.io.IOException
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.IntNode
import org.marketdesignresearch.mechlib.domain.Good
import org.marketdesignresearch.mechlib.domain.SimpleGood
import org.marketdesignresearch.mechlib.domain.bidder.XORBidder
import org.springframework.boot.jackson.JsonComponent


@JsonComponent
class GoodJsonSerialization {
    // FIXME: For now, deserialize Good as SimpleGood. Think about a general way, e.g. like the DomainWrapper Interface
    class SimpleGoodJsonDeserializer : JsonDeserializer<Good>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun deserialize(jsonParser: JsonParser,
                        deserializationContext: DeserializationContext): Good {

            val treeNode: TreeNode = jsonParser.codec.readTree(jsonParser)
            val id = treeNode.get("id") as TextNode
            val dummyGood = treeNode.get("dummyGood") as? BooleanNode
            val availability = treeNode.get("availability") as? IntNode
            return SimpleGood(id.asText(), availability?.asInt() ?: 1, dummyGood?.asBoolean() ?: false)
        }
    }
}