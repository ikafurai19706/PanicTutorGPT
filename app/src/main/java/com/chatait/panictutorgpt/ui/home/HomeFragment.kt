package com.chatait.panictutorgpt.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.chatait.panictutorgpt.MainActivity
import com.chatait.panictutorgpt.R
import com.chatait.panictutorgpt.data.GeminiService
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
        Pair(10, 20),  // 2限
        Pair(11, 40),   // 3限
        Pair(13, 20),  // 4限
        Pair(14, 40),  // 5限
        Pair(16, 0)    // 6限
    )
    // ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲ ▲▲▲

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            updateCountdownView()
            handler.postDelayed(this, 10)
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
            // テスト一覧をチェックリスト形式で表示
            showStudyChecklistDialog()
        }

        // 勉強履歴表示ボタン
        binding.studyHistoryButton.setOnClickListener {
            showStudyHistoryDialog()
        }

        // 長押しでAPIキー設定ダイアログを表示
        binding.registerButton.setOnLongClickListener {
            (activity as? MainActivity)?.showApiKeySettingDialog()
            true
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
        val sdf = SimpleDateFormat("yyyy/MM/dd\nHH:mm:ss.SS", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        binding.dateTimeText.text = sdf.format(currentTime)
    }

    private fun showApiKeySettingDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_api_key_setting, null)
        val editTextApiKey = dialogView.findViewById<android.widget.EditText>(R.id.editTextApiKey)
        val buttonSave = dialogView.findViewById<android.widget.Button>(R.id.buttonSaveApiKey)
        val textViewStatus = dialogView.findViewById<android.widget.TextView>(R.id.textViewStatus)

        val geminiService = GeminiService(requireContext())

        // 現在の設定状態を表示
        if (geminiService.isApiKeySet()) {
            textViewStatus.text = "APIキーが設定されています"
            textViewStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            textViewStatus.text = "APIキーが設定されていません"
            textViewStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Google Gemini API設定")
            .setView(dialogView)
            .setNegativeButton("キャンセル", null)
            .create()

        buttonSave.setOnClickListener {
            val apiKey = editTextApiKey.text.toString().trim()
            if (apiKey.isNotEmpty()) {
                geminiService.saveApiKey(apiKey)
                Toast.makeText(requireContext(), "APIキーが保存されました", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "APIキーを入力してください", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    /**
     * 勉強完了時にチェックリスト形式でダイアログを表示する
     */
    private fun showStudyChecklistDialog() {
        val schedules = scheduleRepository.loadSchedules()

        if (schedules.isEmpty()) {
            Toast.makeText(requireContext(), "テスト予定が登録されていません", Toast.LENGTH_SHORT).show()
            return
        }

        // 日付ごとにグループ化してチェックリスト項目を作成
        val checklistItems = mutableListOf<ChecklistItem>()

        schedules.sortedBy { it.date }.forEach { scheduleItem ->
            var hasSubjects = false

            // 各科目をチェック
            scheduleItem.subjects.forEachIndexed { index, subject ->
                if (subject.isNotBlank()) {
                    if (!hasSubjects) {
                        // 最初の科目の前に日付ヘッダーを追加
                        checklistItems.add(ChecklistItem(
                            date = scheduleItem.date,
                            subject = "",
                            period = 0,
                            isDateHeader = true
                        ))
                        hasSubjects = true
                    }

                    // 科目のチェックボックスを追加
                    checklistItems.add(ChecklistItem(
                        date = scheduleItem.date,
                        subject = subject,
                        period = index + 1,
                        isChecked = false
                    ))
                }
            }
        }

        if (checklistItems.isEmpty()) {
            Toast.makeText(requireContext(), "勉強できる科目が見つかりません", Toast.LENGTH_SHORT).show()
            return
        }

        // ダイアログを表示
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_study_checklist, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewChecklist)
        val buttonClose = dialogView.findViewById<android.widget.Button>(R.id.buttonCloseChecklist)
        val buttonSave = dialogView.findViewById<android.widget.Button>(R.id.buttonSaveProgress)

        // チェックリストアダプターをセット
        val checklistAdapter = ChecklistAdapter(checklistItems)
        recyclerView.adapter = checklistAdapter
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("📚 勉強した科目を選択")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 完了ボタン
        buttonClose.setOnClickListener {
            dialog.dismiss()
        }

        // 保存ボタン
        buttonSave.setOnClickListener {
            val checkedItems = checklistAdapter.getCheckedItems()
            if (checkedItems.isNotEmpty()) {
                // 勉強記録を保存
                val studyRepository = com.chatait.panictutorgpt.data.StudyRepository(requireContext())
                checkedItems.forEach { item ->
                    val studyRecord = com.chatait.panictutorgpt.data.StudyRecord(
                        date = item.date,
                        subject = item.subject,
                        period = item.period
                    )
                    studyRepository.saveStudyRecord(studyRecord)
                }

                val studiedSubjects = checkedItems.joinToString("、") { "${it.date} ${it.period}限: ${it.subject}" }
                Toast.makeText(
                    requireContext(),
                    "お疲れさまでした！\n勉強した科目: $studiedSubjects",
                    Toast.LENGTH_LONG
                ).show()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "勉強した科目を選択してください", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showStudyHistoryDialog() {
        val studyRepository = com.chatait.panictutorgpt.data.StudyRepository(requireContext())
        val studyRecords = studyRepository.getAllStudyRecords()

        if (studyRecords.isEmpty()) {
            Toast.makeText(requireContext(), "勉強履歴がありません", Toast.LENGTH_SHORT).show()
            return
        }

        // 勉強履歴を表示するダイアログを作成
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_study_history, null)
        val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerViewStudyHistory)
        val buttonClose = dialogView.findViewById<android.widget.Button>(R.id.buttonCloseHistory)

        // 勉強履歴アダプターをセット
        val historyAdapter = StudyHistoryAdapter(studyRecords)
        recyclerView.adapter = historyAdapter
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("📖 勉強履歴")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 閉じるボタン
        buttonClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTimeRunnable)
        _binding = null
    }
}
