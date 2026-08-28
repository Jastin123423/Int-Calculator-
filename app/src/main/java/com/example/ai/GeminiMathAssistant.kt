package com.example.ai

import com.example.BuildConfig
import com.example.domain.calculator.ExpressionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

object GeminiMathAssistant {

    suspend fun askMathAssistant(query: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineFallbackResponse(query)
        }

        try {
            val response = RetrofitClient.service.generateContent(
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = "You are CalcPro AI Math Assistant. Answer clearly, concisely, step-by-step with clean markdown math notation:\n\n$query"))
                        )
                    )
                )
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            text ?: getOfflineFallbackResponse(query)
        } catch (e: Exception) {
            getOfflineFallbackResponse(query)
        }
    }

    private fun getOfflineFallbackResponse(query: String): String {
        val evaluated = ExpressionParser.evaluate(query)
        return if (evaluated is com.example.domain.calculator.CalculationResult.Success) {
            "### Solution:\n\n**Expression:** `$query`  \n**Result:** `${evaluated.formattedValue}`\n\n*(Calculated offline with CalcPro high-precision math engine)*"
        } else {
            "### CalcPro Offline Assistant\n\nI can solve math expressions directly offline! For instance, try:\n- `15% of 850`\n- `solve 2x + 5 = 15`\n- `25 * 4 + 10`"
        }
    }

    data class GeminiRequest(
        val contents: List<GeminiContent>
    )

    data class GeminiContent(
        val parts: List<GeminiPart>
    )

    data class GeminiPart(
        val text: String
    )

    data class GeminiResponse(
        val candidates: List<GeminiCandidate>? = null
    )

    data class GeminiCandidate(
        val content: GeminiContent? = null
    )

    interface GeminiApiService {
        @POST("v1beta/models/gemini-3.5-flash:generateContent")
        suspend fun generateContent(
            @Query("key") apiKey: String,
            @Body request: GeminiRequest
        ): GeminiResponse
    }

    private object RetrofitClient {
        private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val service: GeminiApiService by lazy {
            Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(GeminiApiService::class.java)
        }
    }
}
