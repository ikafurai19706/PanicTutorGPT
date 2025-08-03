package com.chatait.panictutorgpt

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
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
import kotlinx.coroutines.withContext

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

        // バックグラウンド脅迫サービスを開始
        startThreatService()
    }

    private fun startThreatService() {
        val serviceIntent = Intent(this, com.chatait.panictutorgpt.service.ThreatNotificationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Log.d("MainActivity", "脅迫サービスを開始しました")
    }

    private fun checkAndShowApiKeyDialog() {
        if (!geminiService.isApiKeySet()) {
            showApiKeyDialog()
        }
    }

    private fun showApiKeyDialog() {
        val editText = EditText(this)
        editText.hint = "Gemini API キーを入力してください"

        AlertDialog.Builder(this)
            .setTitle("API キー設定")
            .setMessage("Gemini API を使用してリマインダーメッセージを生成するため、API キーが必要です。")
            .setView(editText)
            .setPositiveButton("設定") { _, _ ->
                val apiKey = editText.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    geminiService.saveApiKey(apiKey)
                    Toast.makeText(this, "API キーが設定されました", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "API キーが空です", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("後で") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "デフォルトメッセージを使用します", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Default Channel"
            val descriptionText = "Default notification channel"
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
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    fun sendTestReminder() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = geminiService.generateReminderMessage()

                withContext(Dispatchers.Main) {
                    showNotification("テスト勉強リマインダー", message)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "リマインダー送信時にエラーが発生しました: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    showNotification("テスト勉強リマインダー", "勉強を始めましょう！")
                }
            }
        }
    }

    fun showNotification(title: String, message: String) {
        // 通知をタップした時にアプリを起動するためのIntent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // 通知タップ時の動作を設定

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(Random().nextInt(), builder.build())
            }
        }
    }

    fun showApiKeySettingDialog() {
        showApiKeyDialog()
    }

    override fun onPause() {
        super.onPause()
        // アプリがバックグラウンドに移行した時に脅迫メッセージをチェック
        checkAndSendThreatMessage()
    }

    override fun onDestroy() {
        super.onDestroy()
        // アプリが完全に閉じられた時にも脅迫メッセージをチェック
        checkAndSendThreatMessage()
    }

    private fun checkAndSendThreatMessage() {
        val scheduleRepository = com.chatait.panictutorgpt.data.ScheduleRepository(this)
        val studyRepository = com.chatait.panictutorgpt.data.StudyRepository(this)
        val schedules = scheduleRepository.loadSchedules()

        // 1週間以内のすべての科目が完了していたら通知をオフ
        if (studyRepository.areAllSubjectsWithinOneWeekCompleted(schedules)) {
            Log.d("MainActivity", "1週間以内のすべての科目が完了済みのため通知をスキップ")
            return
        }

        val upcomingTests = getTestsWithinOneWeek(scheduleRepository)

        if (upcomingTests.isNotEmpty()) {
            sendThreatNotification(upcomingTests)
        }
    }

    private fun getTestsWithinOneWeek(scheduleRepository: com.chatait.panictutorgpt.data.ScheduleRepository): List<Pair<String, List<String>>> {
        val schedules = scheduleRepository.loadSchedules()
        val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
        val currentTime = System.currentTimeMillis()
        val oneWeekFromNow = currentTime + (7 * 24 * 60 * 60 * 1000)

        return schedules.mapNotNull { scheduleItem ->
            try {
                val testDate = dateFormat.parse(scheduleItem.date)?.time
                if (testDate != null && testDate in currentTime..oneWeekFromNow) {
                    val subjects = scheduleItem.subjects.filter { it.isNotBlank() }
                    if (subjects.isNotEmpty()) {
                        Pair(scheduleItem.date, subjects)
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun sendThreatNotification(upcomingTests: List<Pair<String, List<String>>>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val testInfo = upcomingTests.joinToString("、") { (date, subjects) ->
                    "${date}: ${subjects.joinToString("、")}"
                }

                val threatMessage = geminiService.generateThreatMessage(testInfo)

                withContext(Dispatchers.Main) {
                    showNotification("🚨 緊急警告 🚨", threatMessage)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "脅迫メッセージ送信時にエラー: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    val fallbackMessage = generateFallbackThreatMessage(upcomingTests)
                    showNotification("🚨 緊急警告 🚨", fallbackMessage)
                }
            }
        }
    }

    private fun generateFallbackThreatMessage(upcomingTests: List<Pair<String, List<String>>>): String {
        val subjects = upcomingTests.flatMap { it.second }.distinct()
        val threatMessages = listOf(
            "逃げても無駄です...${subjects.joinToString("、")}のテストが迫っています...",
            "アプリを閉じても現実は変わりません。${subjects.joinToString("、")}の準備はできていますか？",
            "恐怖の時間が始まります...${subjects.joinToString("、")}で良い点を取れなければ...",
            "運命の日が近づいています。${subjects.joinToString("、")}の勉強を怠れば後悔することになります...",
            "暗闇の中で${subjects.joinToString("、")}のテストがあなたを見つめています...",
            "時間は容赦なく過ぎています...${subjects.joinToString("、")}の準備はまだ終わっていませんね？"
        )
        return threatMessages.random()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_reset_all_data -> {
                showResetAllDataDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showResetAllDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("全履歴リセット")
            .setMessage("スケジュールと勉強記録をすべて削除しますか？\n（APIキーは保持されます）")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("削除") { _, _ ->
                resetAllData()
            }
            .setNegativeButton("キャンセル") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun resetAllData() {
        try {
            // スケジュールデータを削除
            val scheduleRepository = com.chatait.panictutorgpt.data.ScheduleRepository(this)
            scheduleRepository.clearAllSchedules()

            // 勉強記録を削除
            val studyRepository = com.chatait.panictutorgpt.data.StudyRepository(this)
            studyRepository.clearAllStudyRecords()

            Toast.makeText(this, "全履歴を削除しました", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "全履歴削除が完了しました")

        } catch (e: Exception) {
            Log.e("MainActivity", "履歴削除中にエラーが発生しました: ${e.message}", e)
            Toast.makeText(this, "削除中にエラーが発生しました", Toast.LENGTH_SHORT).show()
        }
    }
}