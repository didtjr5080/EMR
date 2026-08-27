package com.example.ditoapp

data class FaceExpressionResult(
    val expression: String,
    val smileScore: Float,
    val blinkScore: Float,
    val jawOpenScore: Float,
    val browDownScore: Float,
    val faceDetected: Boolean
)