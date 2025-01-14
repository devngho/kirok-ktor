package io.github.devngho.kirokktor

import io.github.devngho.kirok.RetrieverData
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass

@KirokKtorDSLMarker
class KtorRetrieveServer {
    private val retrieverMap = mutableMapOf<Pair<KClass<*>, KClass<*>>, suspend (RetrieverData) -> Any>()
    private val intentMap = mutableMapOf<Triple<String, KClass<*>, KClass<*>>, suspend Any.(RetrieverData) -> Any>()
    private var convertException: suspend (Throwable) -> Throwable = { Exception("Failed to process call.") }
    private val dataNameAndSerializerMap = mutableMapOf<String, KSerializer<*>>()
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        explicitNulls = false
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <T: Any, U: RetrieverData> retrieve(clazz: KClass<T>, dataClazz: KClass<U>, block: suspend (U) -> T) {
        dataNameAndSerializerMap[clazz.qualifiedName!!] = clazz.serializer()
        dataNameAndSerializerMap[dataClazz.qualifiedName!!] = dataClazz.serializer()
        retrieverMap[clazz to dataClazz] = block as suspend (RetrieverData) -> Any
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <T: Any> retrieve(clazz: KClass<T>, block: suspend () -> T) {
        dataNameAndSerializerMap[clazz.qualifiedName!!] = clazz.serializer()
        retrieverMap[clazz to Unit::class] = block as suspend (RetrieverData) -> Any
    }

    @OptIn(InternalSerializationApi::class)
    @Suppress("UNCHECKED_CAST")
    fun <T: Any, U: RetrieverData> intent(name: String, clazz: KClass<T>, dataClazz: KClass<U>, block: suspend T.(U) -> T) {
        dataNameAndSerializerMap[clazz.qualifiedName!!] = clazz.serializer()
        dataNameAndSerializerMap[dataClazz.qualifiedName!!] = dataClazz.serializer()
        intentMap[Triple(name, clazz, dataClazz)] = block as suspend Any.(RetrieverData) -> Any
    }

    @KirokKtorDSLMarker
    class ClassAbout<T : Any>(val clazz: KClass<T>, val server: KtorRetrieveServer) {
        @KirokKtorDSLMarker
        inline fun <reified U: RetrieverData> retrieve(noinline block: suspend (@KirokKtorDSLMarker Unit).(U) -> T) {
            server.retrieve(clazz, U::class) { block(Unit, it) }
        }

        @KirokKtorDSLMarker
        fun retrieve(block: suspend (@KirokKtorDSLMarker Unit).() -> T) {
            server.retrieve(clazz) { block(Unit) }
        }

        @KirokKtorDSLMarker
        inline fun <reified U: RetrieverData> intent(name: String, noinline block: suspend (@KirokKtorDSLMarker T).(U) -> T) {
            server.intent(name, clazz, U::class, block)
        }
    }

    @KirokKtorDSLMarker
    inline fun <reified T: Any> define(noinline block: (@KirokKtorDSLMarker ClassAbout<T>).() -> Unit) = ClassAbout(T::class, this).apply(block)

    @KirokKtorDSLMarker
    fun convertException(block: suspend (Throwable) -> Throwable) {
        convertException = block
    }

    fun build(): EmbeddedServer<*, *> {
        return embeddedServer(Netty, port = 8080) {
            routing {
                post("/retrieve") {
                    val result = processCall(call.receiveText())

                    when (result) {
                        is CallResult.Success -> call.respond(result.data)
                        is CallResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                        is CallResult.UnknownError -> call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }
    }

    sealed interface CallResult {
        data class Success(val data: Any): CallResult
        data object NotFound : CallResult
        data class UnknownError(@Transient val throwable: Throwable): CallResult
    }


    suspend fun processCall(data: String): CallResult = runCatching {
        val body = json.parseToJsonElement(data).jsonObject
        val type = json.decodeFromJsonElement<KtorRetrieverData.RetrieverType>(body["type"]!!)
        val clazz = body["class"]!!.jsonPrimitive.content
        val dataClazz = body["data_class"]!!.jsonPrimitive.content

        val dataSerializer = dataNameAndSerializerMap[dataClazz] ?: return@runCatching CallResult.NotFound

        val dataParsed = json.decodeFromJsonElement(dataSerializer, body["data"]!!) as RetrieverData

        when(type) {
            KtorRetrieverData.RetrieverType.RETRIEVE -> {
                val retriever =
                    retrieverMap.filterKeys { it.first.qualifiedName == clazz && it.second.qualifiedName == dataClazz }.values.firstOrNull() ?: return@runCatching CallResult.NotFound

                retriever(dataParsed)
            }
            KtorRetrieverData.RetrieverType.INTENT -> {
                val modelSerializer = dataNameAndSerializerMap[clazz]
                val model = json.decodeFromJsonElement(modelSerializer!!, body["model"]!!)!!

                val intentName = body["intent"]!!.jsonPrimitive.content

                val intent =
                    intentMap.filterKeys { it.first == intentName && it.second.qualifiedName == clazz && it.third.qualifiedName == dataClazz }.values.firstOrNull() ?: return@runCatching CallResult.NotFound

                intent(model, dataParsed)
            }
        }
    }.recoverCatching {
        throw convertException(it)
    }.let {
        if (it.isSuccess) {
            CallResult.Success(it.getOrThrow())
        } else {
            CallResult.UnknownError(it.exceptionOrNull()!!)
        }
    }

    companion object {
        inline operator fun invoke(block: KtorRetrieveServer.() -> Unit): KtorRetrieveServer {
            return KtorRetrieveServer().apply(block)
        }

        inline fun Route.retrieverServer(server: KtorRetrieveServer.() -> Unit) {
            val instance = KtorRetrieveServer().apply(server)

            post {
                val result = instance.processCall(call.receiveText())

                when (result) {
                    is CallResult.Success -> call.respond(result.data)
                    is CallResult.NotFound -> call.respond(HttpStatusCode.NotFound, "Not found")
                    is CallResult.UnknownError -> call.respond(HttpStatusCode.InternalServerError, "Internal server error")
                }
            }
        }

        inline fun Route.retrieverServer(route: String, crossinline server: KtorRetrieveServer.() -> Unit) = route(route) { retrieverServer(server) }
        inline fun Route.retrieverServer(route: Regex, crossinline server: KtorRetrieveServer.() -> Unit) = route(route) { retrieverServer(server) }
    }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
@DslMarker
annotation class KirokKtorDSLMarker