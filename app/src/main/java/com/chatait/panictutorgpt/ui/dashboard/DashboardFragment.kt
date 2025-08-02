package com.chatait.panictutorgpt.ui.dashboard

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chatait.panictutorgpt.R
import com.chatait.panictutorgpt.databinding.FragmentDashboardBinding
import java.util.Calendar

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 予定追加ボタンのクリックリスナー
        binding.dashboardEntryButton.setOnClickListener {
            showAddScheduleForm()
        }

        dashboardViewModel.text.observe(viewLifecycleOwner) {

        }
        return root
    }

    // 日付と教科名を同じフォームで入力する関数
    private fun showAddScheduleForm() {
        val context = requireContext()
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_add_schedule, null)
        val dateEdit = dialogView.findViewById<EditText>(R.id.editTextDate)
        val subjectEdit = dialogView.findViewById<EditText>(R.id.editTextSubject)

        val calendar = Calendar.getInstance()
        var selectedYear = calendar.get(Calendar.YEAR)
        var selectedMonth = calendar.get(Calendar.MONTH)
        var selectedDay = calendar.get(Calendar.DAY_OF_MONTH)

        dateEdit.setOnClickListener {
            val datePicker = DatePickerDialog(context, { _, year, month, dayOfMonth ->
                selectedYear = year
                selectedMonth = month
                selectedDay = dayOfMonth
                dateEdit.setText("%04d/%02d/%02d".format(year, month + 1, dayOfMonth))
            }, selectedYear, selectedMonth, selectedDay)
            datePicker.show()
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("テストの予定を追加")
            .setView(dialogView)
            .setPositiveButton("追加") { _, _ ->
                val date = dateEdit.text.toString()
                val subject = subjectEdit.text.toString()
                // ここで予定データを保存する処理を追加可能
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