package com.chatait.panictutorgpt.data

import android.content.Context
import android.content.SharedPreferences
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

class OpenAIService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private fun getApiKey(): String? {
        val prefs = context.getSharedPreferences("openai_config", Context.MODE_PRIVATE)
        return prefs.getString("api_key", null)
    }

    fun saveApiKey(apiKey: String) {
        val prefs = context.getSharedPreferences("openai_config", Context.MODE_PRIVATE)
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
                    Log.w("OpenAIService", "APIキーが設定されていません。フォールバックメッセージを使用します。")
                    return@withContext getFallbackMessage()
                }

                Log.d("OpenAIService", "OpenAI APIを呼び出します...")

                val requestBody = OpenAIRequest(
                    model = "gpt-4o-mini",
                    messages = listOf(
                        Message(
                            role = "system",
                            content = "あなたは学生のテスト勉強を促す、少し脅迫的だが愛のあるリマインダーアシスタントです。日本語で、短く（30文字以内）、勉強を促す緊迫感のあるメッセージを生成してください。"
                        ),
                        Message(
                            role = "user",
                            content = "テスト勉強をサボっている学生への短いリマインダーメッセージを生成してください。"
                        )
                    ),
                    max_tokens = 50,
                    temperature = 0.8
                )

                val json = gson.toJson(requestBody)
                val body = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    responseBody?.let {
                        val openAIResponse = gson.fromJson(it, OpenAIResponse::class.java)
                        val aiMessage = openAIResponse.choices.firstOrNull()?.message?.content?.trim()
                        if (!aiMessage.isNullOrEmpty()) {
                            Log.d("OpenAIService", "OpenAI APIからメッセージを取得しました: $aiMessage")
                            return@withContext aiMessage
                        }
                    }
                }

                Log.e("OpenAIService", "API request failed: ${response.code} - ${response.message}")
                Log.e("OpenAIService", "Response body: ${response.body?.string()}")
                return@withContext getFallbackMessage()

            } catch (e: IOException) {
                Log.e("OpenAIService", "Network error: ${e.message}")
                return@withContext getFallbackMessage()
            } catch (e: Exception) {
                Log.e("OpenAIService", "Unexpected error: ${e.message}")
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
            "勉強しないと...後悔することになりますよ？"
        )
        val selectedMessage = fallbackMessages.random()
        Log.d("OpenAIService", "フォールバックメッセージを使用: $selectedMessage")
        return selectedMessage
    }

    // データクラス定義
    data class OpenAIRequest(
        val model: String,
        val messages: List<Message>,
        val max_tokens: Int,
        val temperature: Double
    )

    data class Message(
        val role: String,
        val content: String
    )

    data class OpenAIResponse(
        val choices: List<Choice>
    )

    data class Choice(
        val message: MessageResponse
    )

    data class MessageResponse(
        val content: String
    )
}
