package com.chatait.panictutorgpt

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.*
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.chatait.panictutorgpt.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Random
import com.chatait.panictutorgpt.data.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "default_channel"
    private lateinit var binding: ActivityMainBinding
    private lateinit var geminiService: GeminiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // GeminiServiceを初期化
        geminiService = GeminiService(this)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        createNotificationChannel()
        requestNotificationPermission()

        // アプリ起動時にAPIキーが設定されていない場合はダイアログを表示
        checkAndShowApiKeyDialog()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "PanicTutor通知"
            val descriptionText = "テスト予定の通知チャンネル"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    fun showNotification() {
        // Android 13以降で通知パーミッションが付与されているか確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e("MainActivity", "通知パーミッションが付与されていません")
                Toast.makeText(this, "通知パーミッションが必要です", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val title = "⚠️ テスト予定リマインダー"
        Log.d("MainActivity", "リマインダー通知の生成を開始します")

        // OpenAI APIを使って動的にメッセージを生成
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val text = getRandomScaryMessage()
                Log.d("MainActivity", "メッセージ生成完了: $text")

                val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntentRequestCode = System.currentTimeMillis().toInt()
                val pendingIntent: PendingIntent = PendingIntent.getActivity(
                    this@MainActivity,
                    pendingIntentRequestCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val builder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)

                try {
                    with(NotificationManagerCompat.from(this@MainActivity)) {
                        val notificationId = System.currentTimeMillis().toInt()
                        notify(notificationId, builder.build())
                        Log.d("MainActivity", "通知を正常に送信しました (ID: $notificationId)")
                    }

                    saveNotificationToHistory(title, text)
                    Log.d("MainActivity", "通知履歴を保存しました")

                } catch (e: SecurityException) {
                    Log.e("MainActivity", "通知送信に失敗しました - セキュリティエラー: ${e.message}")
                    Toast.makeText(this@MainActivity, "通知送信に失敗しました", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("MainActivity", "通知送信中に予期しないエラーが発生しました: ${e.message}", e)
                    Toast.makeText(this@MainActivity, "通知送信エラーが発生しました", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "リマインダー生成中にエラーが発生しました: ${e.message}", e)
                Toast.makeText(this@MainActivity, "リマインダー生成に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getRandomScaryMessage(): String {
        return geminiService.generateScaryMessage()
    }

    private fun saveNotificationToHistory(title: String, message: String) {
        val prefs = getSharedPreferences("notification_history", MODE_PRIVATE)
        val history = prefs.getStringSet("history", null)?.toMutableSet() ?: mutableSetOf()
        val timestamp = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        history.add("[$timestamp] $title|$message")

        prefs.edit()
            .putStringSet("history", history)
            .apply()
    }

    private fun checkAndShowApiKeyDialog() {
        if (!geminiService.isApiKeySet()) {
            showApiKeySettingDialog()
        }
    }

    private fun showApiKeySettingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_api_key_setting, null)
        val editTextApiKey = dialogView.findViewById<android.widget.EditText>(R.id.editTextApiKey)
        val buttonSave = dialogView.findViewById<android.widget.Button>(R.id.buttonSaveApiKey)
        val textViewStatus = dialogView.findViewById<android.widget.TextView>(R.id.textViewStatus)

        // 現在の設定状態を表示
        if (geminiService.isApiKeySet()) {
            textViewStatus.text = "APIキーが設定されています"
            textViewStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            textViewStatus.text = "APIキーが設定されていません"
            textViewStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Google Gemini API設定")
            .setMessage("AIが生成する個性的な通知メッセージを受け取るには、Google Gemini APIキーの設定が必要です。")
            .setView(dialogView)
            .setNegativeButton("後で設定") { _, _ ->
                Toast.makeText(this, "ホーム画面の通知ボタンを長押しで後から設定できます", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .create()

        buttonSave.setOnClickListener {
            val apiKey = editTextApiKey.text.toString().trim()
            if (apiKey.isNotEmpty()) {
                geminiService.saveApiKey(apiKey)
                Toast.makeText(this, "APIキーが保存されました", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "APIキーを入力してください", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // パーミッションが付与された
                } else {
                    // パーミッションが拒否された
                }
                return
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}