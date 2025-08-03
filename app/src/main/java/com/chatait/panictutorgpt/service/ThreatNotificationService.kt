package com.chatait.panictutorgpt.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chatait.panictutorgpt.MainActivity
import com.chatait.panictutorgpt.R
import com.chatait.panictutorgpt.data.GeminiService
import com.chatait.panictutorgpt.data.ScheduleRepository
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ThreatNotificationService : Service() {

    private val CHANNEL_ID = "threat_channel"
    private val NOTIFICATION_ID = 1001
    private lateinit var geminiService: GeminiService
    private lateinit var scheduleRepository: ScheduleRepository
    private var serviceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        geminiService = GeminiService(this)
        scheduleRepository = ScheduleRepository(this)
        createNotificationChannel()
        Log.d("ThreatService", "脅迫通知サービスが開始されました")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        startThreatMonitoring()
        return START_STICKY // サービスが強制終了されても自動的に再起動
    }

    private fun startForegroundService() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("監視システム")
            .setContentText("テスト期間中の逃亡を監視中...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startThreatMonitoring() {
        serviceJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                try {
                    checkAndSendThreatMessage()
                    delay(TimeUnit.MINUTES.toMillis(30)) // 30分ごとにチェック
                } catch (e: Exception) {
                    Log.e("ThreatService", "脅迫メッセージチェック中にエラー: ${e.message}")
                    delay(TimeUnit.MINUTES.toMillis(10)) // エラー時は10分後に再試行
                }
            }
        }
    }

    private suspend fun checkAndSendThreatMessage() {
        val studyRepository = com.chatait.panictutorgpt.data.StudyRepository(this)
        val schedules = scheduleRepository.loadSchedules()

        // 1週間以内のすべての科目が完了していたら通知をオフ
        if (studyRepository.areAllSubjectsWithinOneWeekCompleted(schedules)) {
            Log.d("ThreatService", "1週間以内のすべての科目が完了済みのため通知をスキップ")
            return
        }

        val upcomingTests = getTestsWithinOneWeek()

        if (upcomingTests.isNotEmpty()) {
            // 勉強記録をチェックして、すべて勉強済みの日は除外
            val testsNeedingStudy = upcomingTests.filter { (date, subjects) ->
                // その日のスケジュールアイテムを取得
                val scheduleItem = scheduleRepository.loadSchedules().find { it.date == date }
                scheduleItem?.let { schedule ->
                    // すべての科目が勉強済みでない場合のみ通知対象
                    !studyRepository.areAllSubjectsStudiedForDate(schedule)
                } ?: true // スケジュールが見つからない場合は通知対象
            }

            if (testsNeedingStudy.isNotEmpty()) {
                Log.d("ThreatService", "勉強が必要なテスト: $testsNeedingStudy")
                sendThreatNotification(testsNeedingStudy)
            } else {
                Log.d("ThreatService", "すべてのテストの勉強が完了しています")
            }
        } else {
            Log.d("ThreatService", "1週間以内のテストはありません")
        }
    }

    private fun getTestsWithinOneWeek(): List<Pair<String, List<String>>> {
        val schedules = scheduleRepository.loadSchedules()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
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

    private suspend fun sendThreatNotification(upcomingTests: List<Pair<String, List<String>>>) {
        try {
            val testInfo = upcomingTests.joinToString("、") { (date, subjects) ->
                "${date}: ${subjects.joinToString("、")}"
            }

            val threatMessage = geminiService.generateThreatMessage(testInfo)
            showThreatNotification("🚨 逃亡者発見 🚨", threatMessage)

        } catch (e: Exception) {
            Log.e("ThreatService", "脅迫メッセージ生成エラー: ${e.message}")
            val fallbackMessage = generateFallbackThreatMessage(upcomingTests)
            showThreatNotification("🚨 逃亡者発見 🚨", fallbackMessage)
        }
    }

    private fun showThreatNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000)) // 振動パターン
            .setLights(0xFFFF0000.toInt(), 1000, 1000) // 赤色LEDライト
            .build()

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@ThreatNotificationService,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(Random().nextInt(1000, 9999), notification)
            }
        }

        Log.d("ThreatService", "脅迫通知を送信しました: $message")
    }

    private fun generateFallbackThreatMessage(upcomingTests: List<Pair<String, List<String>>>): String {
        val subjects = upcomingTests.flatMap { it.second }.distinct()
        val threatMessages = listOf(
            "💀 どこに隠れても無駄です...${subjects.joinToString("、")}のテストから逃れることはできません 💀",
            "🔥 アプリを閉じて逃げたつもりですか？${subjects.joinToString("、")}の恐怖があなたを追いかけます 🔥",
            "👻 暗闇の中から${subjects.joinToString("、")}のテストがあなたを見つめています... 👻",
            "⚡ 運命から逃れることはできません...${subjects.joinToString("、")}の審判の時が来ました ⚡",
            "🌪️ 嵐のような${subjects.joinToString("、")}のテストがあなたを襲います...覚悟はできていますか？ 🌪️",
            "💥 時間は刻一刻と過ぎています...${subjects.joinToString("、")}の準備を怠った代償を払う時です 💥"
        )
        return threatMessages.random()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "脅迫通知チャンネル"
            val descriptionText = "テスト期間中の脅迫メッセージ"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFFFF0000.toInt()
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        Log.d("ThreatService", "脅迫通知サービスが停止されました")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
