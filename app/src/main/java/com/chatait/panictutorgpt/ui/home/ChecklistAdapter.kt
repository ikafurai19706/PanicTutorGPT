package com.chatait.panictutorgpt.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatait.panictutorgpt.R

data class ChecklistItem(
    val date: String,
    val subject: String,
    val period: Int,
    var isChecked: Boolean = false,
    val isDateHeader: Boolean = false
)

class ChecklistAdapter(private val items: MutableList<ChecklistItem>) :
    RecyclerView.Adapter<ChecklistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateHeader: TextView = view.findViewById(R.id.textDateHeader)
        val checkboxSubject: CheckBox = view.findViewById(R.id.checkboxSubject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_checklist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (item.isDateHeader) {
            // 日付ヘッダー表示
            holder.dateHeader.visibility = View.VISIBLE
            holder.dateHeader.text = "📅 ${item.date}"
            holder.checkboxSubject.visibility = View.GONE
        } else {
            // 科目チェックボックス表示
            holder.dateHeader.visibility = View.GONE
            holder.checkboxSubject.visibility = View.VISIBLE
            holder.checkboxSubject.text = "${item.period}限: ${item.subject}"
            holder.checkboxSubject.isChecked = item.isChecked

            // チェックボックスのクリックリスナー
            holder.checkboxSubject.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun getCheckedItems(): List<ChecklistItem> {
        return items.filter { it.isChecked && !it.isDateHeader }
    }
}
