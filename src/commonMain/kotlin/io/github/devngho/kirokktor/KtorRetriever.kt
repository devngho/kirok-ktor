package io.github.devngho.kirokktor

import io.github.devngho.kirok.Retriever
import io.github.devngho.kirok.RetrieverInfo
import kotlinx.serialization.Serializable

expect object KtorRetriever: Retriever<KtorRetrieverInfo>

@Serializable
data class KtorRetrieverInfo(
    val path: String,
    val headers: Map<String, String>
): RetrieverInfo