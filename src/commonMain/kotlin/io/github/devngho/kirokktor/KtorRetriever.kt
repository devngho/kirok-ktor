package io.github.devngho.kirokktor

import io.github.devngho.kirok.Retriever
import io.github.devngho.kirok.RetrieverData
import io.github.devngho.kirok.RetrieverInfo
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

expect object KtorRetriever: Retriever<KtorRetrieverInfo> {
    override suspend fun <U : Any> intent(
        info: KtorRetrieverInfo?,
        name: String,
        model: U,
        clazz: KClass<U>,
        dataClazz: KClass<out RetrieverData>,
        clazzName: String,
        dataClazzName: String,
        data: RetrieverData
    ): U

    override suspend fun <U : Any> retrieve(
        info: KtorRetrieverInfo?,
        clazz: KClass<U>,
        dataClazz: KClass<out RetrieverData>,
        clazzName: String,
        dataClazzName: String,
        data: RetrieverData
    ): U
}

@Serializable
data class KtorRetrieverInfo(
    val path: String,
    val headers: Map<String, String>
): RetrieverInfo