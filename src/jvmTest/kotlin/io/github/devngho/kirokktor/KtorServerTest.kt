package io.github.devngho.kirokktor


import io.github.devngho.kirok.RetrieverData
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class KtorServerTest: DescribeSpec({
    val server = KtorRetrieveServer {
        define<TestModel> {
            retrieve<TestModelData> {
                TestModel(it.value)
            }

            retrieve<TestModelData2> {
                TestModel(it.value.toString())
            }

            intent<TestModelData2>("merge") {
                this.copy(value = this.value + it.value.toString())
            }
        }

        define<TestModel2> {
            retrieve<TestModelData2> {
                TestModel2(it.value)
            }

            retrieve<TestModelData> {
                throw Exception("Oops!")
            }
        }
    }

    describe("KtorRetrieveServer") {
        it("생성한 서버를 실행할 수 있다") {
            val s = server.build().start()
            delay(1000)
            s.stop(1000, 1000)
        }
        
        it("모델을 retrieve할 수 있다") {
            val testInputText = """
                {
                    "type": "retrieve",
                    "class": "${TestModel::class.qualifiedName!!}",
                    "data_class": "${TestModelData::class.qualifiedName!!}",
                    "data": {
                        "value": "test"
                    }
                }
            """.trimIndent()

            val output = server.processCall(testInputText)

            output shouldNotBe null
            (output is KtorRetrieveServer.CallResult.Success) shouldBe true
            (output as KtorRetrieveServer.CallResult.Success).data shouldBe TestModel("test")
        }

        it("데이터에 따라 모델을 retrieve할 수 있다") {
            val testInputText = """
                {
                    "type": "retrieve",
                    "class": "${TestModel::class.qualifiedName!!}",
                    "data_class": "${TestModelData2::class.qualifiedName!!}",
                    "data": {
                        "value": 123
                    }
                }
            """.trimIndent()

            val output = server.processCall(testInputText)

            output shouldNotBe null
            (output is KtorRetrieveServer.CallResult.Success) shouldBe true
            (output as KtorRetrieveServer.CallResult.Success).data shouldBe TestModel("123")
        }

        it("여러 모델을 retrieve할 수 있다") {
            val testInputText = """
                {
                    "type": "retrieve",
                    "class": "${TestModel2::class.qualifiedName!!}",
                    "data_class": "${TestModelData2::class.qualifiedName!!}",
                    "data": {
                        "value": 123
                    }
                }
            """.trimIndent()

            val output = server.processCall(testInputText)

            output shouldNotBe null
            (output is KtorRetrieveServer.CallResult.Success) shouldBe true
            (output as KtorRetrieveServer.CallResult.Success).data shouldBe TestModel2(123)
        }

        it("모델의 intent를 처리할 수 있다") {
            val testInputText = """
                {
                    "type": "intent",
                    "class": "${TestModel::class.qualifiedName!!}",
                    "data_class": "${TestModelData2::class.qualifiedName!!}",
                    "data": {
                        "value": 123
                    },
                    "model": ${Json.encodeToString(TestModel.serializer(), TestModel("123"))},
                    "intent": "merge"
                }
            """.trimIndent()

            val output = server.processCall(testInputText)

            output shouldNotBe null
            (output is KtorRetrieveServer.CallResult.Success) shouldBe true
            (output as KtorRetrieveServer.CallResult.Success).data shouldBe TestModel("123123")
        }

        it("intent에서 오류가 발생하면 처리할 수 있다") {
            val testInputText = """
                {
                    "type": "retrieve",
                    "class": "${TestModel2::class.qualifiedName!!}",
                    "data_class": "${TestModelData::class.qualifiedName!!}",
                    "data": {
                        "value": "test"
                    }
                }
            """.trimIndent()

            val output = server.processCall(testInputText)

            output shouldNotBe null
            (output is KtorRetrieveServer.CallResult.UnknownError) shouldBe true
        }
    }
}) {
    @Serializable
    data class TestModel(
        val value: String
    )

    @Serializable
    data class TestModelData(
        val value: String
    ): RetrieverData

    @Serializable
    data class TestModel2(
        val value: Int
    )

    @Serializable
    data class TestModelData2(
        val value: Int
    ): RetrieverData
}