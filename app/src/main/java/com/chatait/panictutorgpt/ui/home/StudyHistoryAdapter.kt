package com.chatait.panictutorgpt.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatait.panictutorgpt.R
import com.chatait.panictutorgpt.data.StudyRecord
import java.text.SimpleDateFormat
import java.util.*

class StudyHistoryAdapter(private val studyRecords: List<StudyRecord>) :
    RecyclerView.Adapter<StudyHistoryAdapter.ViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textStudyDate: TextView = view.findViewById(R.id.textStudyDate)
        val textSubjectInfo: TextView = view.findViewById(R.id.textSubjectInfo)
        val textStudyTime: TextView = view.findViewById(R.id.textStudyTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = studyRecords[position]

        holder.textStudyDate.text = "📅 ${record.date}"
        holder.textSubjectInfo.text = "${record.period}限: ${record.subject}"
        holder.textStudyTime.text = "勉強完了: ${timeFormat.format(Date(record.timestamp))}"
    }

    override fun getItemCount(): Int = studyRecords.size
}
