package com.chatait.panictutorgpt.ui.dashboard

data class ScheduleItem(
    val date: String, // 例: "2025/08/03"
    val subjects: List<String>, // 1限～6限の科目名
    val id: String = java.util.UUID.randomUUID().toString()
)
