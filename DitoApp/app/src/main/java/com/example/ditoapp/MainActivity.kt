package com.example.ditoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class AppScreen {
    Controller,
    FaceExpression
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DitoApp()
                }
            }
        }
    }
}

@Composable
fun DitoApp() {
    var currentScreen by remember {
        mutableStateOf(AppScreen.Controller)
    }

    when (currentScreen) {
        AppScreen.Controller -> {
            ControllerScreen(
                onMoveToFaceExpression = {
                    currentScreen = AppScreen.FaceExpression
                }
            )
        }

        AppScreen.FaceExpression -> {
            FaceExpressionScreen(
                onBack = {
                    currentScreen = AppScreen.Controller
                },
                onExpressionChanged = {
                    // 표정 제어 기능은 사용하지 않으므로 비워둠
                }
            )
        }
    }
}