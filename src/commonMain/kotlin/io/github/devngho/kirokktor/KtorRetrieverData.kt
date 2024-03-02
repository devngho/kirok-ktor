package io.github.devngho.kirokktor

import io.github.devngho.kirokktor.KtorRetrieverData.RetrieverType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

@Serializable
data class KtorRetrieverData internal constructor(
    val type: RetrieverType,
    val `class`: String,
    @SerialName("data_class")
    val dataClass: String,
    val data: JsonElement,
    /**
     * for [RetrieverType.INTENT]
     * serialized model(type is same as `class`)
     */
    val model: JsonElement?,
    /**
     * for [RetrieverType.RETRIEVE]
     * to classify the intent
     */
    val intent: String?,
    val headers: Map<String, String> = emptyMap()
) {
    @Serializable(with = RetrieverType.RetrieverTypeSerializer::class)
    enum class RetrieverType {
        RETRIEVE,
        INTENT;

        object RetrieverTypeSerializer: KSerializer<RetrieverType> {
            override val descriptor: SerialDescriptor
                get() = PrimitiveSerialDescriptor("RetrieverType", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): RetrieverType = RetrieverType.valueOf(decoder.decodeString().uppercase())

            override fun serialize(encoder: Encoder, value: RetrieverType) = encoder.encodeString(value.name.lowercase())
        }
    }
}