package io.github.devngho.kirokktor

import io.github.devngho.kirok.Retriever
import io.github.devngho.kirok.RetrieverData
import kotlin.reflect.KClass

actual object KtorRetriever : Retriever<KtorRetrieverInfo> {
    override suspend fun <U : Any> retrieve(
        info: KtorRetrieverInfo?,
        clazz: KClass<U>,
        dataClazz: KClass<out RetrieverData>,
        clazzName: String,
        dataClazzName: String,
        data: RetrieverData
    ): U {
        throw NotImplementedError("KtorRetriever can't be used in JVM.")
    }

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
        throw NotImplementedError("KtorRetriever can't be used in JVM.")
    }
}