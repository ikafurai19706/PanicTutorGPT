package com.chatait.panictutorgpt.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chatait.panictutorgpt.R

class ScheduleAdapter(
    private val items: MutableList<ScheduleItem>,
    private val onDeleteClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.scheduleDate)
        val subjectsText: TextView = view.findViewById(R.id.scheduleSubjects)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.dateText.text = item.date
        holder.subjectsText.text = item.subjects.withIndex()
            .filter { it.value.isNotBlank() }
            .joinToString("\n") { (i, s) -> "${i+1}限: $s" }

        // 5秒長押しで削除確認ダイアログを表示（削除不可期間でも強制削除可能）
        var longPressHandler: Runnable? = null

        // 元の背景色を保存
        val originalBackground = holder.itemView.background

        holder.itemView.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // 長押し開始時に背景色を変更
                    view.setBackgroundColor(ContextCompat.getColor(view.context, android.R.color.holo_red_light))

                    longPressHandler = Runnable {
                        // 5秒長押しの場合は削除不可期間でも強制削除確認
                        // 元の背景色に戻す
                        view.background = originalBackground

                        AlertDialog.Builder(holder.itemView.context)
                            .setTitle("予定を削除")
                            .setMessage("本当に削除しますか？この操作は取り消せません。")
                            .setPositiveButton("削除") { _, _ ->
                                onDeleteClick?.invoke(item.date)
                            }
                            .setNegativeButton("キャンセル", null)
                            .show()
                    }
                    view.postDelayed(longPressHandler, 5000) // 5秒後に実行
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    // 指を離したときに元の背景色に戻す
                    view.background = originalBackground
                    longPressHandler?.let { view.removeCallbacks(it) }
                }
            }
            true
        }
    }

    override fun getItemCount(): Int = items.size
}
