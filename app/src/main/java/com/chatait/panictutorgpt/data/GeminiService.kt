package com.chatait.panictutorgpt.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
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
        prefs.edit {
            putString("api_key", apiKey)
        }
    }

    fun isApiKeySet(): Boolean {
        return !getApiKey().isNullOrEmpty()
    }

    suspend fun generateReminderMessage(): String {
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
                                        あなたはテスト勉強になかなか手を付けない大学生のテスト勉強を焦らせて促す、脅迫的なリマインダーアシスタントです。  日本語で、短く、テスト勉強を促すメッセージを1つだけ生成してください。文字数は30字までの文章にしてください。生成する文章はテストを一週間以内に控えている大学生が読むとします。読んだ大学生が今から始めないとまずいなと思うようなものにしてください。今からだと間に合わないや、あなたは単位を落とすであろうという旨の否定的な冷めたようなものはやめてください。焦らせてやる気を出させるような文章を生成してください。口調は少しだけ柔らかいものにしてください。なるべく複数回生成しても被らないようなものにしてください。
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
                    .addHeader("X-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    responseBody?.let { responseString ->
                        try {
                            val geminiResponse = gson.fromJson(responseString, GeminiResponse::class.java)
                            val aiMessage = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                            if (!aiMessage.isNullOrEmpty()) {
                                Log.d("GeminiService", "Gemini APIからメッセージを取得しました: $aiMessage")
                                return@withContext aiMessage
                            }
                        } catch (e: Exception) {
                            Log.e("GeminiService", "レスポンス解析エラー: ${e.message}")
                            Log.e("GeminiService", "Response body: $responseString")
                        }
                    }
                } else {
                    Log.e("GeminiService", "API request failed: ${response.code} - ${response.message}")
                    Log.e("GeminiService", "Response body: ${response.body?.string()}")
                }

                return@withContext getFallbackMessage()

            } catch (e: IOException) {
                Log.e("GeminiService", "ネットワークエラー: ${e.message}")
                return@withContext getFallbackMessage()
            } catch (e: Exception) {
                Log.e("GeminiService", "予期しないエラー: ${e.message}")
                return@withContext getFallbackMessage()
            }
        }
    }

    suspend fun generateThreatMessage(testInfo: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getApiKey()
                if (apiKey.isNullOrEmpty()) {
                    Log.w("GeminiService", "APIキーが設定されていません。フォールバック脅迫メッセージを使用します。")
                    return@withContext getThreatFallbackMessage(testInfo)
                }

                Log.d("GeminiService", "Gemini 2.0 Flash APIで脅迫メッセージを生成します...")

                val requestBody = GeminiRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(
                                    text = "あなたは学生に恐怖を与える悪魔的なテスト監督です。以下のテスト情報を元に、日本語で非常に脅迫的で恐ろしいメッセージを生成してください。アプリを閉じて逃げようとした学生を追い詰めるような、背筋が凍るような内容にしてください。絵文字を使って恐怖感を演出し、50文字以内で作成してください。\n\nテスト情報: $testInfo"
                                )
                            )
                        )
                    )
                )

                val json = gson.toJson(requestBody)
                val body = json.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
                    .addHeader("X-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    responseBody?.let { responseString ->
                        try {
                            val geminiResponse = gson.fromJson(responseString, GeminiResponse::class.java)
                            val aiMessage = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                            if (!aiMessage.isNullOrEmpty()) {
                                Log.d("GeminiService", "Gemini APIから脅迫メッセージを取得しました: $aiMessage")
                                return@withContext aiMessage
                            }
                        } catch (e: Exception) {
                            Log.e("GeminiService", "脅迫メッセージレスポンス解析エラー: ${e.message}")
                            Log.e("GeminiService", "Response body: $responseString")
                        }
                    }
                } else {
                    Log.e("GeminiService", "脅迫メッセージAPI request failed: ${response.code} - ${response.message}")
                    Log.e("GeminiService", "Response body: ${response.body?.string()}")
                }

                return@withContext getThreatFallbackMessage(testInfo)

            } catch (e: IOException) {
                Log.e("GeminiService", "脅迫メッセージネットワークエラー: ${e.message}")
                return@withContext getThreatFallbackMessage(testInfo)
            } catch (e: Exception) {
                Log.e("GeminiService", "脅迫メッセージ予期しないエラー: ${e.message}")
                return@withContext getThreatFallbackMessage(testInfo)
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
        Log.d("GeminiService", "フォールバックメッセージを使用: $selectedMessage")
        return selectedMessage
    }

    private fun getThreatFallbackMessage(testInfo: String): String {
        val threatMessages = listOf(
            "💀 逃げても無駄...テストの恐怖があなたを追いかけます 💀",
            "🔥 アプリを閉じても現実は変わらない...準備はできていますか？ 🔥",
            "👻 暗闇からテストがあなたを見つめています... 👻",
            "⚡ 運命の時が近づいています...震えて待て ⚡",
            "🌪️ 嵐のようなテストがやってきます...覚悟はいいですか？ 🌪️",
            "💥 時間は容赦なく過ぎています...後悔の時が来る 💥"
        )
        val selectedMessage = threatMessages.random()
        Log.d("GeminiService", "フォールバック脅迫メッセージを使用: $selectedMessage")
        return selectedMessage
    }

    // Gemini API用データクラス定義
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
