package com.chatait.panictutorgpt.ui.notifications

import android.content.Context // Context をインポート
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter // ArrayAdapter をインポート
import android.widget.ListView     // ListView をインポート
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chatait.panictutorgpt.R // Rクラスをインポート (R.layout.fragment_notifications と R.id.notification_list のため)
import com.chatait.panictutorgpt.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var listView: ListView // listView はここで宣言

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // View Binding を使用して TextView を取得
        val textView: TextView = binding.textNotifications // binding.textNotifications を使用
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // View Binding を使用して ListView を取得し、履歴を読み込む
        // fragment_notifications.xml に notification_list という ID の ListView があることを確認してください
        listView = binding.notificationList // binding.notificationList を使用 (IDが一致する場合)
        // もし fragment_notifications.xml のルートが _binding.root で、
        // その中に R.id.notification_list がある場合は、以下のようにすることもできます。
        // listView = root.findViewById(R.id.notification_list)
        loadNotificationHistory()

        return root
    }

    private fun loadNotificationHistory() {
        // SharedPreferences から通知履歴を読み込む
        val prefs = requireContext().getSharedPreferences("notification_history", Context.MODE_PRIVATE)
        val historySet = prefs.getStringSet("history", emptySet())

        // 履歴をリストに変換 (タイトルとメッセージを結合)
        val historyList = historySet?.map {
            val parts = it.split("|||")
            val title = parts.getOrNull(0) ?: "タイトルなし" // デフォルト値を設定
            val message = parts.getOrNull(1) ?: "メッセージなし" // デフォルト値を設定
            "$title\n$message"
        } ?: listOf("通知はまだありません。") // 履歴がない場合のデフォルトメッセージ

        // ArrayAdapter を使用して ListView に履歴を表示
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, historyList)
        listView.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // メモリリークを防ぐために binding を null に設定
    }
}