package com.chatait.panictutorgpt.ui.notifications

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chatait.panictutorgpt.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // ListView を初期化し、通知履歴を表示
        listView = binding.notificationList
        loadNotificationHistory()

        return root
    }

    private fun loadNotificationHistory() {
        val prefs = requireContext().getSharedPreferences("notification_history", Context.MODE_PRIVATE)
        val historySet = prefs.getStringSet("history", emptySet())

        val historyList = if (historySet.isNullOrEmpty()) {
            listOf("通知履歴はまだありません。\nHomeタブの「Entry」ボタンで通知を送信してください。")
        } else {
            historySet.map { historyItem ->
                val parts = historyItem.split("|")
                val titleWithTime = parts.getOrNull(0) ?: "タイトルなし"
                val message = parts.getOrNull(1) ?: "メッセージなし"
                "$titleWithTime\n$message"
            }.sortedDescending() // 新しい順に表示
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, historyList)
        listView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // フラグメントが表示されるたびに履歴を更新
        loadNotificationHistory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}