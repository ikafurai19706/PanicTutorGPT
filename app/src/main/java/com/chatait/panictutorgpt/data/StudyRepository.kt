package com.chatait.panictutorgpt.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class StudyRecord(
    val date: String,           // テスト日付
    val subject: String,        // 科目名
    val period: Int,           // 時限
    val studyDate: String,     // 勉強した日付 (新規追加)
    val timestamp: Long = System.currentTimeMillis()
)

class StudyRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("study_records", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

    fun saveStudyRecord(studyRecord: StudyRecord) {
        val existingRecords = getStudyRecords().toMutableList()

        // 同じテスト日付・科目・時限・勉強日付の記録があれば更新、なければ追加
        val existingIndex = existingRecords.indexOfFirst {
            it.date == studyRecord.date &&
            it.subject == studyRecord.subject &&
            it.period == studyRecord.period &&
            it.studyDate == studyRecord.studyDate  // 勉強日付も比較に追加
        }

        if (existingIndex != -1) {
            // 同じ日に同じ科目を勉強した記録があれば更新
            existingRecords[existingIndex] = studyRecord
        } else {
            // 新しい日の勉強記録として追加
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

        // 今日勉強した記録から、指定されたテスト日付・科目・時限に一致するものを探す
        return allStudyRecords.any { record ->
            record.studyDate == today &&  // 今日勉強した
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
                record.studyDate == today &&  // 今日勉強した
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

    fun getStudyRecordsForStudyDate(studyDate: String): List<StudyRecord> {
        // 指定した勉強日の記録を取得
        return getStudyRecords().filter { it.studyDate == studyDate }
    }

    fun getStudyHistoryByDate(): Map<String, List<StudyRecord>> {
        // 勉強日別にグループ化した履歴を取得
        return getStudyRecords()
            .groupBy { it.studyDate }
            .toSortedMap(compareByDescending { it })
    }

    fun areAllSubjectsWithinOneWeekCompleted(schedules: List<com.chatait.panictutorgpt.ui.dashboard.ScheduleItem>): Boolean {
        val today = dateFormat.format(Date())
        val currentTime = System.currentTimeMillis()
        val oneWeekFromNow = currentTime + (7 * 24 * 60 * 60 * 1000)

        // 1週間以内のテスト科目を取得
        val subjectsWithinOneWeek = schedules.flatMap { scheduleItem ->
            try {
                val testDate = dateFormat.parse(scheduleItem.date)?.time
                if (testDate != null && testDate in currentTime..oneWeekFromNow) {
                    scheduleItem.subjects.withIndex()
                        .filter { it.value.isNotBlank() }
                        .map { (index, subject) ->
                            Triple(scheduleItem.date, subject, index + 1)
                        }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        if (subjectsWithinOneWeek.isEmpty()) return false

        val allStudyRecords = getStudyRecords()

        // 1週間以内のすべての科目が今日勉強されているかチェック
        return subjectsWithinOneWeek.all { (date, subject, period) ->
            allStudyRecords.any { record ->
                record.studyDate == today &&  // 今日勉強した
                record.date == date &&        // テスト日付が一致
                record.subject == subject &&  // 科目が一致
                record.period == period       // 時限が一致
            }
        }
    }
}
