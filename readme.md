[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.devngho/kirok-svelte-binding/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.devngho/kirok-svelte-binding)

# kirok-ktor

![kirok](https://kirok.nghodev.com/favicon.png)

✅ [kirok](https://github.com/devngho/kirok) 공식 HTTP([Ktor](https://ktor.io)) 리트리버 & 서버

```kts
kotlin {
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation("io.github.devngho:kirok-ktor:[version]")
            }
        }
    }
}
```

```kotlin
@Model
@RetrieveWith(KtorRetriever::class)
data class Model(
    val name: String,
    val age: Int
) {
    companion object {
        @Init(useRetriever = true)
        fun init() {}
      
      @RetrieverInfo
        fun retrieverInfo(): RetrieverInfo {
          return RetrieverInfo(
              path = "http://example.com/api",
              headers = emptyMap()
          )
      }
    }
}
```

## 지원하는 기능

- [x] retrieve
- [x] intent
- [x] 서버