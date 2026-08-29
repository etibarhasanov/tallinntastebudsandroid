package ee.tallinntastebuds.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/** `data/taxonomy.json`: the closed set of type chips, with a label per language. */
@Serializable
data class Taxonomy(val types: List<PlaceType> = emptyList()) {
    companion object {
        val empty = Taxonomy()
    }
}

/**
 * A type chip. Every key in the object other than `id` is a language code
 * mapped to that language's label, so the set of languages is whatever the site
 * publishes rather than anything this app declares.
 */
@Serializable(with = PlaceTypeSerializer::class)
data class PlaceType(
    val id: String,
    val labels: Map<String, String>,
) {
    fun labelIn(lang: String): String = labels[lang] ?: labels["en"] ?: id
}

object PlaceTypeSerializer : KSerializer<PlaceType> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PlaceType")

    override fun deserialize(decoder: Decoder): PlaceType {
        val json = decoder as? JsonDecoder
            ?: throw IllegalStateException("a type chip is only ever read from JSON")
        val obj = json.decodeJsonElement().jsonObject
        var id = ""
        val labels = mutableMapOf<String, String>()
        for ((key, element) in obj) {
            // A non-string value is a label this app cannot draw. Skipping it
            // costs one chip its name; throwing would cost the reader every chip.
            val text = (element as? JsonPrimitive)?.takeIf { it.isString }?.content ?: continue
            if (key == "id") id = text else labels[key] = text
        }
        require(id.isNotEmpty()) { "taxonomy type without an id" }
        return PlaceType(id, labels)
    }

    override fun serialize(encoder: Encoder, value: PlaceType) {
        val json = encoder as? JsonEncoder
            ?: throw IllegalStateException("a type chip is only ever written as JSON")
        val fields = buildMap {
            put("id", JsonPrimitive(value.id))
            value.labels.forEach { (lang, label) -> put(lang, JsonPrimitive(label)) }
        }
        json.encodeJsonElement(JsonObject(fields))
    }
}
