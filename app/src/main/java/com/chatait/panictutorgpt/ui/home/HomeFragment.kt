package com.chatait.panictutorgpt.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chatait.panictutorgpt.MainActivity
import com.chatait.panictutorgpt.databinding.FragmentHomeBinding // Bindingクラスはレイアウトファイル名に依存します
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    // XMLファイル名が fragment_home.xml なら FragmentHomeBinding になります
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 10)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        // 初回表示
        updateDateTime()
        // 更新処理を開始
        handler.post(updateTimeRunnable)

        // 登録ボタンのクリック処理
        binding.registerButton.setOnClickListener {
            (activity as? MainActivity)?.showNotification()
            Toast.makeText(context, "リマインダー通知を送信しました！", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    private fun updateDateTime() {
        // デジタル時計の更新 (小数点以下2桁表示)
        // アナログ時計を動かすためのコードは不要です！
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