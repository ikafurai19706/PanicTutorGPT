package com.chatait.panictutorgpt.data

data class QuizQuestion(
    val question: String,
    val correctAnswer: String,
    val options: List<String> = emptyList() // 選択肢がある場合
)

data class QuizResult(
    val subject: String,
    val question: QuizQuestion,
    val userAnswer: String,
    val isCorrect: Boolean
)
