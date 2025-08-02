package com.chatait.panictutorgpt

import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.*
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.chatait.panictutorgpt.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Random // Randomをインポート

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "default_channel"
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        createNotificationChannel()
        requestNotificationPermission()

        binding.buttonNotification.setOnClickListener {
            showNotification() // こちらの showNotification が呼び出される
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Channel"
            val descriptionText = "My" // 文字列リソースから取得推奨
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // --- 1つ目の showNotification メソッド（削除対象）---
    /*
    private fun showNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 必要に応じて変更
            .setContentTitle("通知タイトル")
            .setContentText("これは通知のテストです")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            // Android 13以降で通知パーミッションが付与されているか確認
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notify(1, builder.build())
                } else {
                    // パーミッションがない場合の処理（例：ユーザーに通知するなど）
                }
            } else {
                notify(1, builder.build())
            }
        }
    }
    */
    // --- ここまで1つ目の showNotification メソッド ---

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                    // ここでユーザーにパーミッションが必要な理由を説明するUIを表示します。
                    // 例えば、AlertDialog を使用できます。
                    // new AlertDialog.Builder(this)
                    // .setTitle("通知の許可が必要です")
                    // .setMessage("このアプリの重要な機能を利用するには、通知の許可が必要です。許可しますか？")
                    // .setPositiveButton("許可する") { _, _ ->
                    // ActivityCompat.requestPermissions(
                    // this,
                    // arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    // NOTIFICATION_PERMISSION_REQUEST_CODE
                    // )
                    // }
                    // .setNegativeButton("許可しない", null)
                    // .show()
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    // この showNotification メソッドを残します
    private fun showNotification() {
        // Android 13以降で通知パーミッションが付与されているか確認
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // パーミッションがない場合、ユーザーに通知するか、リクエストを促すなどの処理
                // (例: Toast.makeText(this, "通知の送信にはパーミッションが必要です。", Toast.LENGTH_SHORT).show())
                // requestNotificationPermission() // 再度リクエストを試みることも可能
                return // パーミッションがない場合はここで処理を中断
            }
        }

        val title = "通知タイトル" // 必要に応じて変更
        val text = getRandomScaryMessage()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        // ユニークなリクエストコードを PendingIntent に設定
        val pendingIntentRequestCode = System.currentTimeMillis().toInt()
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            pendingIntentRequestCode, // リクエストコードをユニークにする
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // 適切なアイコンに置き換えてください
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // タップで通知を消す

        with(NotificationManagerCompat.from(this)) {
            // 通知IDもユニークにすることで、複数の通知が正しく表示されるようにします
            val notificationId = System.currentTimeMillis().toInt()
            notify(notificationId, builder.build())
        }

        saveNotificationToHistory(title, text)
    }

    // getRandomScaryMessage のダミー実装 (実際のロジックに置き換えてください)
    private fun getRandomScaryMessage(): String {
        val messages = listOf(
            "締め切りが迫っています！",
            "見て見ぬふりはできません...",
            "あなたの行動が見られています。",
            "時間は刻一刻と過ぎています。",
            "本当にそれでいいのですか？"
        )
        return messages[Random().nextInt(messages.size)]
    }

    private fun saveNotificationToHistory(title: String, message: String) {
        val prefs = getSharedPreferences("notification_history", MODE_PRIVATE)
        // 既存の履歴を取得し、nullの場合は新しい空のSetを作成
        val history = prefs.getStringSet("history", null)?.toMutableSet() ?: mutableSetOf()
        history.add("$title|||$message") // 新しい通知を追加
        prefs.edit().putStringSet("history", history).apply()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // パーミッションが付与された！
                    // ここで通知を表示するなどのアクションを実行できます
                    // 例: showNotification()
                } else {
                    // パーミッションが拒否された
                    // ユーザーに機能が制限されることを伝えるなどの処理
                    // 例: Toast.makeText(this, "通知パーミッションが拒否されたため、一部機能が利用できません。", Toast.LENGTH_LONG).show()
                }
                return
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}