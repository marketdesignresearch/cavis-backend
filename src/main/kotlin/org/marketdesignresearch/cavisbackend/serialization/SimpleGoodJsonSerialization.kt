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
import org.marketdesignresearch.mechlib.domain.SimpleGood
import org.springframework.boot.jackson.JsonComponent


@JsonComponent
class SimpleGoodJsonSerialization {

    class SimpleGoodJsonSerializer : JsonSerializer<SimpleGood>() {

        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(simpleGood: SimpleGood, jsonGenerator: JsonGenerator,
                               serializerProvider: SerializerProvider) {

            jsonGenerator.writeStartObject()
            jsonGenerator.writeStringField("name", simpleGood.name)
            jsonGenerator.writeStringField("id", simpleGood.uuid.toString())
            jsonGenerator.writeNumberField("availability", simpleGood.available())
            jsonGenerator.writeBooleanField("dummyGood", simpleGood.isDummyGood)
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
}