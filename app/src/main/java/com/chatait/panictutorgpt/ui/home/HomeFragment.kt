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
import androidx.lifecycle.lifecycleScope
import com.chatait.panictutorgpt.MainActivity
import com.chatait.panictutorgpt.R
import com.chatait.panictutorgpt.data.GeminiService
import com.chatait.panictutorgpt.data.ScheduleRepository
import com.chatait.panictutorgpt.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
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

        // 現在の日付を取得
        val currentDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        // 過去のテストを除外し、現在・未来のテストのみを対象とする
        val futureSchedules = schedules.filter { scheduleItem ->
            try {
                val testDate = dateFormat.parse(scheduleItem.date)
                testDate != null && (testDate.after(currentDate) || testDate == currentDate)
            } catch (e: Exception) {
                false // 日付解析エラーの場合は除外
            }
        }

        if (futureSchedules.isEmpty()) {
            Toast.makeText(requireContext(), "勉強できる未来のテスト予定がありません", Toast.LENGTH_SHORT).show()
            return
        }

        // 日付ごとにグループ化してチェックリスト項目を作成
        val checklistItems = mutableListOf<ChecklistItem>()

        futureSchedules.sortedBy { it.date }.forEach { scheduleItem ->
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
                // 確認テストを開始
                startConfirmationQuiz(checkedItems) { completedItems ->
                    // 勉強記録を保存
                    val studyRepository = com.chatait.panictutorgpt.data.StudyRepository(requireContext())
                    val today = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault()).format(java.util.Date())

                    completedItems.forEach { item ->
                        val studyRecord = com.chatait.panictutorgpt.data.StudyRecord(
                            date = item.date,
                            subject = item.subject,
                            period = item.period,
                            studyDate = today
                        )
                        studyRepository.saveStudyRecord(studyRecord)
                    }

                    val studiedSubjects = completedItems.joinToString("、") { "${it.date} ${it.period}限: ${it.subject}" }
                    Toast.makeText(
                        requireContext(),
                        "お疲れさまでした！\n勉強した科目: $studiedSubjects",
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                }
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

    /**
     * 確認テストを開始する
     */
    private fun startConfirmationQuiz(
        checkedItems: List<ChecklistItem>,
        onComplete: (List<ChecklistItem>) -> Unit
    ) {
        if (checkedItems.isEmpty()) {
            onComplete(checkedItems)
            return
        }

        var currentIndex = 0
        val wrongAnswers = mutableListOf<Pair<ChecklistItem, String>>() // 間違えた問題と解答を記録

        fun showNextQuiz() {
            if (currentIndex >= checkedItems.size) {
                // 全ての問題が完了
                if (wrongAnswers.isNotEmpty()) {
                    // 間違えた問題がある場合は罵倒通知を送信
                    sendInsultNotifications(wrongAnswers)
                }
                onComplete(checkedItems)
                return
            }

            val currentItem = checkedItems[currentIndex]
            showQuizDialog(currentItem) { userAnswer, isCorrect ->
                if (!isCorrect && userAnswer.isNotEmpty()) {
                    wrongAnswers.add(Pair(currentItem, userAnswer))
                }
                currentIndex++
                showNextQuiz()
            }
        }

        showNextQuiz()
    }

    /**
     * 個別の確認テストダイアログを表示
     */
    private fun showQuizDialog(
        item: ChecklistItem,
        onAnswered: (userAnswer: String, isCorrect: Boolean) -> Unit
    ) {
        val geminiService = GeminiService(requireContext())

        // ローディングダイアログを表示
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("確認テスト準備中...")
            .setMessage("${item.subject}の問題を生成しています...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // 問題を生成
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                val quizQuestion = geminiService.generateQuizQuestion(item.subject)
                loadingDialog.dismiss()

                if (quizQuestion != null) {
                    showQuizQuestionDialog(item, quizQuestion, onAnswered)
                } else {
                    // フォールバック問題
                    val fallbackQuestion = com.chatait.panictutorgpt.data.GeminiService.QuizQuestion(
                        question = "${item.subject}について重要なポイントを1つ説明してください。",
                        correctAnswer = "基本的な概念や原理について正確に説明すること"
                    )
                    showQuizQuestionDialog(item, fallbackQuestion, onAnswered)
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "問題生成に失敗しました", Toast.LENGTH_SHORT).show()
                onAnswered("", true) // エラーの場合は正解として扱う
            }
        }
    }

    /**
     * 問題ダイアログを表示
     */
    private fun showQuizQuestionDialog(
        item: ChecklistItem,
        question: com.chatait.panictutorgpt.data.GeminiService.QuizQuestion,
        onAnswered: (userAnswer: String, isCorrect: Boolean) -> Unit
    ) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quiz_question, null)
        val questionText = dialogView.findViewById<android.widget.TextView>(R.id.textQuestion)
        val answerEdit = dialogView.findViewById<android.widget.EditText>(R.id.editAnswer)

        questionText.text = "【${item.subject}】\n${question.question}"

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("確認テスト")
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("解答") { _, _ ->
                val userAnswer = answerEdit.text.toString().trim()
                val isCorrect = checkAnswer(userAnswer, question.correctAnswer)

                // 結果を表示
                showQuizResultDialog(item, question, userAnswer, isCorrect) {
                    onAnswered(userAnswer, isCorrect)
                }
            }
            .setNegativeButton("スキップ") { _, _ ->
                onAnswered("", true) // スキップは正解として扱う
            }
            .create()

        dialog.show()
    }

    /**
     * 解答結果ダイアログを表示
     */
    private fun showQuizResultDialog(
        item: ChecklistItem,
        question: com.chatait.panictutorgpt.data.GeminiService.QuizQuestion,
        userAnswer: String,
        isCorrect: Boolean,
        onDismiss: () -> Unit
    ) {
        val resultMessage = if (isCorrect) {
            "🎉 正解です！\n\nあなたの解答: $userAnswer"
        } else {
            "❌ 残念...不正解です\n\nあなたの解答: $userAnswer\n正解: ${question.correctAnswer}"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("${item.subject} - 結果")
            .setMessage(resultMessage)
            .setPositiveButton("次へ") { _, _ ->
                onDismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 解答の正誤判定（簡易版）
     */
    private fun checkAnswer(userAnswer: String, correctAnswer: String): Boolean {
        if (userAnswer.isEmpty()) return false

        // 簡易的な判定（実際のプロダクトではより高度な判定が必要）
        val userWords = userAnswer.lowercase().split(Regex("[\\s、。,.]")).filter { it.isNotEmpty() }
        val correctWords = correctAnswer.lowercase().split(Regex("[\\s、。,.]")).filter { it.isNotEmpty() }

        // 正解の単語の30%以上が含まれていれば正解とする
        val matchCount = correctWords.count { correctWord ->
            userWords.any { userWord ->
                userWord.contains(correctWord) || correctWord.contains(userWord)
            }
        }

        return matchCount >= (correctWords.size * 0.3).coerceAtLeast(1.0)
    }

    /**
     * 罵倒通知を送信
     */
    private fun sendInsultNotifications(wrongAnswers: List<Pair<ChecklistItem, String>>) {
        android.util.Log.d("HomeFragment", "sendInsultNotifications called with ${wrongAnswers.size} wrong answers")

        lifecycleScope.launch {
            try {
                val geminiService = GeminiService(requireContext())

                wrongAnswers.forEach { (item, wrongAnswer) ->
                    android.util.Log.d("HomeFragment", "Processing wrong answer for subject: ${item.subject}")

                    val correctAnswer = "正解例" // 実際には問題から取得
                    val insultMessages = geminiService.generateInsultMessages(item.subject, wrongAnswer, correctAnswer)

                    android.util.Log.d("HomeFragment", "Generated ${insultMessages.size} insult messages")

                    // 10個の罵倒通知を連続で送信（間隔なし）
                    insultMessages.forEachIndexed { index, message ->
                        android.util.Log.d("HomeFragment", "Sending insult notification ${index + 1}: $message")

                        val mainActivity = activity as? MainActivity
                        if (mainActivity != null) {
                            mainActivity.showNotification(
                                "💀 ${item.subject} - 不正解通知 ${index + 1}/10",
                                message
                            )
                            android.util.Log.d("HomeFragment", "Notification sent successfully")
                        } else {
                            android.util.Log.e("HomeFragment", "MainActivity is null, cannot send notification")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "罵倒通知送信エラー: ${e.message}", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTimeRunnable)
        _binding = null
    }
}
