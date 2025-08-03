package com.chatait.panictutorgpt.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun getApiKey(): String? {
        val prefs = context.getSharedPreferences("gemini_config", Context.MODE_PRIVATE)
        return prefs.getString("api_key", null)
    }

    fun saveApiKey(apiKey: String) {
        val prefs = context.getSharedPreferences("gemini_config", Context.MODE_PRIVATE)
        prefs.edit().putString("api_key", apiKey).apply()
    }

    fun isApiKeySet(): Boolean {
        return !getApiKey().isNullOrEmpty()
    }

    suspend fun generateScaryMessage(): String {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getApiKey()
                if (apiKey.isNullOrEmpty()) {
                    Log.w("GeminiService", "APIキーが設定されていません。フォールバックメッセージを使用します。")
                    return@withContext getFallbackMessage()
                }

                Log.d("GeminiService", "Gemini 2.0 Flash APIを呼び出します...")

                val requestBody = GeminiRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(
                                    text = """
                                        
                                    """.trimIndent()
                                )
                            )
                        )
                    )
                )

                val json = gson.toJson(requestBody)
                val body = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-goog-api-key", apiKey)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    responseBody?.let {
                        val geminiResponse = gson.fromJson(it, GeminiResponse::class.java)
                        val aiMessage = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                        if (!aiMessage.isNullOrEmpty()) {
                            Log.d("GeminiService", "Gemini 2.0 Flash APIからメッセージを取得しました: $aiMessage")
                            return@withContext aiMessage
                        }
                    }
                }

                Log.e("GeminiService", "API request failed: ${response.code} - ${response.message}")
                Log.e("GeminiService", "Response body: ${response.body?.string()}")
                return@withContext getFallbackMessage()

            } catch (e: IOException) {
                Log.e("GeminiService", "Network error: ${e.message}")
                return@withContext getFallbackMessage()
            } catch (e: Exception) {
                Log.e("GeminiService", "Unexpected error: ${e.message}", e)
                return@withContext getFallbackMessage()
            }
        }
    }

    private fun getFallbackMessage(): String {
        val fallbackMessages = listOf(
            "締め切りが迫っています！今すぐ勉強を始めましょう！",
            "見て見ぬふりはできません...テスト準備は大丈夫ですか？",
            "あなたの勉強状況が気になります。頑張って！",
            "時間は刻一刻と過ぎています。準備はお済みですか？",
            "本当にそれでいいのですか？今から始めれば間に合います！",
            "テスト当日まであとわずか...準備を忘れずに！",
            "勉強しないと...後悔することになりますよ？",
            "もう逃げ場はありません...今すぐ勉強開始！",
            "このままでは本当にヤバいですよ？",
            "テスト結果が心配で夜も眠れません..."
        )
        val selectedMessage = fallbackMessages.random()
        Log.d("GeminiService", "フォールバックメッセージを使用: $selectedMessage")
        return selectedMessage
    }

    // データクラス定義（Gemini 2.0 Flash API用）
    data class GeminiRequest(
        val contents: List<Content>
    )

    data class Content(
        val parts: List<Part>
    )

    data class Part(
        val text: String
    )

    data class GeminiResponse(
        val candidates: List<Candidate>?
    )

    data class Candidate(
        val content: ContentResponse?
    )

    data class ContentResponse(
        val parts: List<PartResponse>?
    )

    data class PartResponse(
        val text: String?
    )
}
