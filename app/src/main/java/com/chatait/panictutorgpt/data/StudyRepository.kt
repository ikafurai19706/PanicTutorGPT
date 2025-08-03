package com.chatait.panictutorgpt.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class StudyRecord(
    val date: String,
    val subject: String,
    val period: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class StudyRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("study_records", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    fun saveStudyRecord(studyRecord: StudyRecord) {
        val existingRecords = getStudyRecords().toMutableList()

        // 同じ日付・科目・時限の記録があれば更新、なければ追加
        val existingIndex = existingRecords.indexOfFirst {
            it.date == studyRecord.date &&
            it.subject == studyRecord.subject &&
            it.period == studyRecord.period
        }

        if (existingIndex != -1) {
            existingRecords[existingIndex] = studyRecord
        } else {
            existingRecords.add(studyRecord)
        }

        val json = gson.toJson(existingRecords)
        prefs.edit().putString("study_records", json).apply()
    }

    fun getStudyRecords(): List<StudyRecord> {
        val json = prefs.getString("study_records", null) ?: return emptyList()
        val type = object : TypeToken<List<StudyRecord>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getAllStudyRecords(): List<StudyRecord> {
        return getStudyRecords().sortedByDescending { it.timestamp }
    }

    fun getStudyRecordsForDate(date: String): List<StudyRecord> {
        return getStudyRecords().filter { it.date == date }
    }

    fun isSubjectStudiedToday(date: String, subject: String, period: Int): Boolean {
        val today = dateFormat.format(Date())
        val allStudyRecords = getStudyRecords()

        // 今日作成された勉強記録から、指定されたテスト日付・科目・時限に一致するものを探す
        return allStudyRecords.any { record ->
            val recordDate = Date(record.timestamp)
            val recordDateString = dateFormat.format(recordDate)

            recordDateString == today &&  // 今日記録された
            record.date == date &&        // テスト日付が一致
            record.subject == subject &&  // 科目が一致
            record.period == period       // 時限が一致
        }
    }

    fun areAllSubjectsStudiedForDate(scheduleItem: com.chatait.panictutorgpt.ui.dashboard.ScheduleItem): Boolean {
        val today = dateFormat.format(Date())
        val allStudyRecords = getStudyRecords()

        // その日の全科目を取得
        val allSubjects = scheduleItem.subjects.withIndex()
            .filter { it.value.isNotBlank() }
            .map { (index, subject) -> Triple(scheduleItem.date, subject, index + 1) }

        if (allSubjects.isEmpty()) return false

        // すべての科目が今日勉強されているかチェック
        return allSubjects.all { (date, subject, period) ->
            allStudyRecords.any { record ->
                val recordDate = Date(record.timestamp)
                val recordDateString = dateFormat.format(recordDate)

                recordDateString == today &&  // 今日記録された
                record.date == date &&        // テスト日付が一致
                record.subject == subject &&  // 科目が一致
                record.period == period       // 時限が一致
            }
        }
    }

    fun clearOldRecords() {
        // 7日以前の記録を削除
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        val recentRecords = getStudyRecords().filter { it.timestamp > sevenDaysAgo }

        val json = gson.toJson(recentRecords)
        prefs.edit().putString("study_records", json).apply()
    }

    fun clearAllStudyRecords() {
        // すべての勉強記録を削除
        prefs.edit().remove("study_records").apply()
    }
}
