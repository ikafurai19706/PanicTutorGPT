package com.chatait.panictutorgpt.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.chatait.panictutorgpt.MainActivity
import com.chatait.panictutorgpt.ui.dashboard.ScheduleItem
import com.chatait.panictutorgpt.data.ScheduleRepository
import com.chatait.panictutorgpt.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var scheduleRepository: ScheduleRepository
    private var closestTestTimeMillis: Long? = null

    // ▼▼▼ 大学の時間割に合わせてここを編集してください ▼▼▼
    // (時, 分) のペアで授業開始時刻を定義
    private val classStartTimes = listOf(
        Pair(9, 0),    // 1限
        Pair(10, 40),  // 2限
        Pair(13, 0),   // 3限
        Pair(14, 40),  // 4限
        Pair(16, 20),  // 5限
        Pair(18, 0)    // 6限
    )
    // ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            updateCountdownView()
            handler.postDelayed(this, 100)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        scheduleRepository = ScheduleRepository(requireContext())

        updateDateTime()
        handler.post(updateTimeRunnable)

        binding.registerButton.setOnClickListener {
            (activity as? MainActivity)?.showNotification()
            Toast.makeText(context, "リマインダー通知を送信しました！", Toast.LENGTH_SHORT).show()
        }
        return root
    }

    override fun onResume() {
        super.onResume()
        findClosestTest()
    }

    /**
     * 最も近いテストの日時を検索し、メンバ変数にセットする
     */
    private fun findClosestTest() {
        val schedules = scheduleRepository.loadSchedules()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        closestTestTimeMillis = schedules
            .mapNotNull { scheduleItem ->
                try {
                    val testDate = Calendar.getInstance().apply { time = dateFormat.parse(scheduleItem.date)!! }
                    // その日の最初のテスト科目の開始時刻を取得
                    val firstTestPeriod = scheduleItem.subjects.indexOfFirst { it.isNotBlank() }
                    if (firstTestPeriod != -1 && firstTestPeriod < classStartTimes.size) {
                        val (hour, minute) = classStartTimes[firstTestPeriod]
                        testDate.set(Calendar.HOUR_OF_DAY, hour)
                        testDate.set(Calendar.MINUTE, minute)
                    }
                    // 現在時刻より後のテストのみを対象とする
                    if (testDate.timeInMillis >= System.currentTimeMillis()) testDate else null
                } catch (e: Exception) {
                    null
                }
            }
            .minByOrNull { it.timeInMillis }
            ?.timeInMillis
    }

    /**
     * カウントダウンの表示を更新する
     */
    private fun updateCountdownView() {
        val targetTime = closestTestTimeMillis ?: run {
            binding.countdownTestText.isVisible = false
            return
        }

        val diffInMillis = targetTime - System.currentTimeMillis()

        if (diffInMillis < 0) {
            binding.countdownTestText.text = "🚨 テスト期間中です！"
            binding.countdownTestText.isVisible = true
            return
        }

        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        if (diffInDays < 3) {
            // 残り3日未満の場合：HH:mm:ss.SS形式で表示
            val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis) % 60
            val millis = (diffInMillis % 1000) / 10

            binding.countdownTestText.text = String.format(
                Locale.getDefault(),
                "テストまで残り %02d:%02d:%02d.%02d",
                hours, minutes, seconds, millis
            )
            binding.countdownTestText.isVisible = true

        } else if (diffInDays <= 7) {
            // 残り7日以内の場合：日数で表示
            binding.countdownTestText.text = "次のテストまで あと${diffInDays + 1}日"
            binding.countdownTestText.isVisible = true

        } else {
            binding.countdownTestText.isVisible = false
        }
    }

    private fun updateDateTime() {
        val currentTime = Calendar.getInstance().time
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SS", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        binding.dateTimeText.text = sdf.format(currentTime)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTimeRunnable)
        _binding = null
    }
}