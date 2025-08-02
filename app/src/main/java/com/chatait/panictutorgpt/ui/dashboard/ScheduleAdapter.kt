package com.chatait.panictutorgpt.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatait.panictutorgpt.R

class ScheduleAdapter(private val items: List<ScheduleItem>) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {
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
    }

    override fun getItemCount(): Int = items.size
}
