package com.chatait.panictutorgpt.ui.dashboard

// 成績の種類を定義するenum
enum class Grade(val displayName: String, val description: String) {
    S("S", "秀"),
    A("A", "優"),
    B("B", "良"),
    C("C", "可"),
    F("F", "不可"),
    Q("Q", "失格"),
    NONE("未入力", "未入力")
}

data class ScheduleItem(
    val date: String, // 例: "2025/08/03"
    val subjects: List<String>, // 1限～6限の科目名
    val grades: List<Grade> = List(6) { Grade.NONE }, // 1限～6限の成績
    val id: String = java.util.UUID.randomUUID().toString()
)

// 表示用のアイテムタイプ
sealed class DisplayItem {
    data class Schedule(val scheduleItem: ScheduleItem) : DisplayItem()
    data class PastTestsHeader(
        val pastTests: List<ScheduleItem>,
        val isExpanded: Boolean = false,
        val id: String = "past_tests_header"
    ) : DisplayItem()
}
