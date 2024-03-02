package io.github.devngho.kirokktor

import io.github.devngho.kirok.Retriever
import io.github.devngho.kirok.RetrieverData
import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

actual object KtorRetriever: Retriever<KtorRetrieverInfo> {
    private val json = Json
    private val client = HttpClient(Js)

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    override suspend fun <U : Any> retrieve(
        info: KtorRetrieverInfo?,
        clazz: KClass<U>,
        dataClazz: KClass<out RetrieverData>,
        clazzName: String,
        dataClazzName: String,
        data: RetrieverData
    ): U {
        return json.decodeFromString(clazz.serializer(), client.post(info!!.path) {
            setBody(json.encodeToString(KtorRetrieverData(KtorRetrieverData.RetrieverType.RETRIEVE, clazzName, dataClazzName, json.encodeToJsonElement(dataClazz.serializer() as KSerializer<RetrieverData>, data), null, null)))

            info.headers.forEach {
                headers.append(it.key, it.value)
            }
        }.bodyAsText())
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    override suspend fun <U : Any> intent(
        info: KtorRetrieverInfo?,
        name: String,
        model: U,
        clazz: KClass<U>,
        dataClazz: KClass<out RetrieverData>,
        clazzName: String,
        dataClazzName: String,
        data: RetrieverData
    ): U {
        return json.decodeFromString(clazz.serializer(), client.post(info!!.path) {
            setBody(json.encodeToString(KtorRetrieverData(KtorRetrieverData.RetrieverType.INTENT, clazzName, dataClazzName, json.encodeToJsonElement(dataClazz.serializer() as KSerializer<RetrieverData>, data), json.encodeToJsonElement(clazz.serializer(), model), name)))

            info.headers.forEach {
                headers.append(it.key, it.value)
            }
        }.bodyAsText())
    }
}