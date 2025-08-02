package com.chatait.panictutorgpt.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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

        // 長押しで削除確認ダイアログを表示
        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("予定を削除")
                .setMessage("${item.date}の予定を削除しますか？")
                .setPositiveButton("削除") { _, _ ->
                    onDeleteClick?.invoke(item.date)
                }
                .setNegativeButton("キャンセル", null)
                .show()
            true
        }
    }

    override fun getItemCount(): Int = items.size
}
