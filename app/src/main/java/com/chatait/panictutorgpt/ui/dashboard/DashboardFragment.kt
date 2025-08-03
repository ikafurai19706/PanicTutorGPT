package com.chatait.panictutorgpt.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatait.panictutorgpt.R
import com.chatait.panictutorgpt.databinding.FragmentDashboardBinding
import com.chatait.panictutorgpt.data.ScheduleRepository
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var scheduleList: MutableList<ScheduleItem>
    private lateinit var adapter: ScheduleAdapter
    private lateinit var scheduleRepository: ScheduleRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // ScheduleRepositoryを初期化
        scheduleRepository = ScheduleRepository(requireContext())

        // 保存されたスケジュールを読み込み
        scheduleList = scheduleRepository.loadSchedules()

        // RecyclerView初期化
        val recyclerView = root.findViewById<RecyclerView>(R.id.scheduleList)
        adapter = ScheduleAdapter(scheduleList) { dateToDelete ->
            // 削除処理
            scheduleRepository.deleteSchedule(dateToDelete)
            scheduleList.clear()
            scheduleList.addAll(scheduleRepository.loadSchedules())
            adapter.notifyDataSetChanged()
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // 予定追加ボタンのクリックリスナー
        binding.dashboardEntryButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val inflater = LayoutInflater.from(requireContext())
            val datePickerView = inflater.inflate(R.layout.dialog_custom_date_picker, null)
            val datePicker = datePickerView.findViewById<DatePicker>(R.id.customDatePicker)
            datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), null)
            // 過去日付を選択不可に
            datePicker.minDate = calendar.timeInMillis

            val dateDialog = AlertDialog.Builder(requireContext())
                .setTitle("テストの日付を選択…")
                .setView(datePickerView)
                .setPositiveButton("次へ") { _, _ ->
                    val year = datePicker.year
                    val month = datePicker.month
                    val day = datePicker.dayOfMonth
                    showAddScheduleForm(year, month, day)
                }
                .setNegativeButton("キャンセル", null)
                .create()
            dateDialog.show()
        }

        dashboardViewModel.text.observe(viewLifecycleOwner) {

        }
        return root
    }

    // 日付と教科名を入力する関数（引数で日付を受け取る）
    private fun showAddScheduleForm(year: Int, month: Int, day: Int) {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_add_schedule, null)
        val subject1 = dialogView.findViewById<EditText>(R.id.editTextSubject1)
        val subject2 = dialogView.findViewById<EditText>(R.id.editTextSubject2)
        val subject3 = dialogView.findViewById<EditText>(R.id.editTextSubject3)
        val subject4 = dialogView.findViewById<EditText>(R.id.editTextSubject4)
        val subject5 = dialogView.findViewById<EditText>(R.id.editTextSubject5)
        val subject6 = dialogView.findViewById<EditText>(R.id.editTextSubject6)
        val errorText = dialogView.findViewById<TextView>(R.id.textError)

        val date = "%04d/%02d/%02d".format(year, month + 1, day)
        // 既存データがあれば初期値セット
        val existing = scheduleList.find { it.date == date }
        if (existing != null) {
            val fields = listOf(subject1, subject2, subject3, subject4, subject5, subject6)
            existing.subjects.forEachIndexed { i, value ->
                if (i < fields.size) fields[i].setText(value)
            }
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("科目名を入力…")
            .setView(dialogView)
            .setPositiveButton("追加", null) // 後でリスナーを設定
            .setNegativeButton("キャンセル", null)
            .create()
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val subjectFields = listOf(subject1, subject2, subject3, subject4, subject5, subject6)
            val updateError = {
                val subjects = subjectFields.map { it.text.toString() }
                errorText.visibility = if (subjects.all { it.isBlank() }) View.VISIBLE else View.GONE
            }
            button.setOnClickListener {
                val subjects = subjectFields.map { it.text.toString() }
                if (subjects.all { it.isBlank() }) {
                    // 既存データがある場合は削除確認ダイアログを表示
                    if (existing != null) {
                        // 一週間前チェック
                        val testDate = Calendar.getInstance()
                        val dateParts = date.split("/")
                        testDate.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt())

                        val today = Calendar.getInstance()
                        val oneWeekFromNow = Calendar.getInstance()
                        oneWeekFromNow.add(Calendar.DAY_OF_YEAR, 7)

                        if (testDate.before(oneWeekFromNow)) {
                            // 一週間前を切っている場合は削除不可
                            val warningDialog = AlertDialog.Builder(context)
                                .setTitle("削除できません")
                                .setMessage("テスト一週間前を切った予定は削除できません。")
                                .setPositiveButton("OK", null)
                                .create()
                            warningDialog.show()
                        } else {
                            // 一週間前を切っていない場合は削除確認
                            val deleteDialog = AlertDialog.Builder(context)
                                .setTitle("確認")
                                .setMessage("この日のテスト予定を削除します。よろしいですか？")
                                .setPositiveButton("削除") { _, _ ->
                                    // 既存のスケジュールを削除
                                    scheduleRepository.deleteSchedule(date)
                                    // UIを更新
                                    scheduleList.clear()
                                    scheduleList.addAll(scheduleRepository.loadSchedules())
                                    adapter.notifyDataSetChanged()
                                    dialog.dismiss()
                                }
                                .setNegativeButton("キャンセル", null)
                                .create()
                            deleteDialog.show()
                        }
                    } else {
                        errorText.visibility = View.VISIBLE
                    }
                } else {
                    errorText.visibility = View.GONE
                    // ScheduleRepositoryを使用してデータを保存
                    val scheduleItem = ScheduleItem(date, subjects)
                    scheduleRepository.addOrUpdateSchedule(scheduleItem)

                    // UIを更新
                    scheduleList.clear()
                    scheduleList.addAll(scheduleRepository.loadSchedules())
                    adapter.notifyDataSetChanged()
                    dialog.dismiss()
                }
            }
            // 入力時にエラー非表示
            subjectFields.forEach { editText ->
                editText.addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                        updateError()
                    }
                    override fun afterTextChanged(s: android.text.Editable?) {}
                })
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}