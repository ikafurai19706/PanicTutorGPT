package com.chatait.panictutorgpt.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.chatait.panictutorgpt.ui.dashboard.ScheduleItem

class ScheduleRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("schedule_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SCHEDULE_PREFIX = "schedule_"
        private const val KEY_SCHEDULE_DATES = "schedule_dates"
    }

    fun saveSchedules(schedules: List<ScheduleItem>) {
        prefs.edit {
            // すべての既存のスケジュールをクリア
            val existingDates = getScheduleDates()
            existingDates.forEach { date ->
                for (i in 0..5) { // 0限〜5限（6時限分）
                    remove("${KEY_SCHEDULE_PREFIX}${date}_subject_$i")
                }
            }

            // 新しいスケジュールを保存
            val dates = schedules.map { it.date }.toSet()
            putStringSet(KEY_SCHEDULE_DATES, dates)

            schedules.forEach { schedule ->
                schedule.subjects.forEachIndexed { index, subject ->
                    putString("${KEY_SCHEDULE_PREFIX}${schedule.date}_subject_$index", subject)
                }
            }
        }
    }

    fun loadSchedules(): MutableList<ScheduleItem> {
        val schedules = mutableListOf<ScheduleItem>()
        val dates = getScheduleDates()

        dates.forEach { date ->
            val subjects = mutableListOf<String>()
            for (i in 0..5) { // 0限〜5限（6時限分）
                val subject = prefs.getString("${KEY_SCHEDULE_PREFIX}${date}_subject_$i", "") ?: ""
                subjects.add(subject)
            }

            // 空でない科目が1つでもあればスケジュールに追加
            if (subjects.any { it.isNotEmpty() }) {
                schedules.add(ScheduleItem(date, subjects))
            }
        }

        return schedules.sortedBy { it.date }.toMutableList()
    }

    fun deleteSchedule(date: String) {
        prefs.edit {
            for (i in 0..5) { // 0限〜5限（6時限分）
                remove("${KEY_SCHEDULE_PREFIX}${date}_subject_$i")
            }

            val dates = getScheduleDates().toMutableSet()
            dates.remove(date)
            putStringSet(KEY_SCHEDULE_DATES, dates)
        }
    }

    fun addOrUpdateSchedule(scheduleItem: ScheduleItem) {
        prefs.edit {
            scheduleItem.subjects.forEachIndexed { index, subject ->
                putString("${KEY_SCHEDULE_PREFIX}${scheduleItem.date}_subject_$index", subject)
            }

            val dates = getScheduleDates().toMutableSet()
            dates.add(scheduleItem.date)
            putStringSet(KEY_SCHEDULE_DATES, dates)
        }
    }

    private fun getScheduleDates(): Set<String> {
        return prefs.getStringSet(KEY_SCHEDULE_DATES, emptySet()) ?: emptySet()
    }
}
