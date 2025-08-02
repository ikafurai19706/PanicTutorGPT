package com.chatait.panictutorgpt.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chatait.panictutorgpt.R
import com.chatait.panictutorgpt.databinding.FragmentDashboardBinding
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val scheduleList = mutableListOf<ScheduleItem>()
    private lateinit var adapter: ScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // RecyclerView初期化
        val recyclerView = root.findViewById<RecyclerView>(R.id.scheduleList)
        adapter = ScheduleAdapter(scheduleList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // 予定追加ボタンのクリックリスナー
        binding.dashboardEntryButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val inflater = LayoutInflater.from(requireContext())
            val datePickerView = inflater.inflate(R.layout.dialog_custom_date_picker, null)
            val datePicker = datePickerView.findViewById<DatePicker>(R.id.customDatePicker)
            datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), null)

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

        val dialog = AlertDialog.Builder(context)
            .setTitle("科目名を入力…")
            .setView(dialogView)
            .setPositiveButton("追加") { _, _ ->
                val date = "%04d/%02d/%02d".format(year, month + 1, day)
                val subjects = listOf(
                    subject1.text.toString(),
                    subject2.text.toString(),
                    subject3.text.toString(),
                    subject4.text.toString(),
                    subject5.text.toString(),
                    subject6.text.toString()
                )
                scheduleList.add(ScheduleItem(date, subjects))
                scheduleList.sortBy { it.date }
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("キャンセル", null)
            .create()
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}