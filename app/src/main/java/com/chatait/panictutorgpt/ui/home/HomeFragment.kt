package com.chatait.panictutorgpt.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.chatait.panictutorgpt.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            updateDateTime()
            handler.postDelayed(this, 1000)
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
        // 1秒ごとに更新
        handler.post(updateTimeRunnable)

        // 登録ボタンのクリック処理
        binding.registerButton.setOnClickListener {
            Toast.makeText(context, "登録されました", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    private fun updateDateTime() {
        val currentTime = Calendar.getInstance().time
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        val formattedTime = sdf.format(currentTime)
        binding.dateTimeText.text = formattedTime
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateTimeRunnable)
        _binding = null
    }
}
