package com.example.ditoapp

import android.annotation.SuppressLint
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean

class FaceExpressionAnalyzer(
    private val onResult: (FaceExpressionResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val isProcessing = AtomicBoolean(false)

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        FaceDetection.getClient(options)
    }

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image

        if (mediaImage == null) {
            onResult(
                FaceExpressionResult(
                    expression = "No Image",
                    smileScore = 0f,
                    blinkScore = 0f,
                    jawOpenScore = 0f,
                    browDownScore = 0f,
                    faceDetected = false
                )
            )

            isProcessing.set(false)
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onResult(
                        FaceExpressionResult(
                            expression = "No Face",
                            smileScore = 0f,
                            blinkScore = 0f,
                            jawOpenScore = 0f,
                            browDownScore = 0f,
                            faceDetected = false
                        )
                    )
                } else {
                    val face = faces.first()

                    val smileScore = face.smilingProbability ?: 0f

                    val leftEyeOpen = face.leftEyeOpenProbability ?: 1f
                    val rightEyeOpen = face.rightEyeOpenProbability ?: 1f

                    val eyeOpenScore = (leftEyeOpen + rightEyeOpen) / 2f
                    val blinkScore = 1f - eyeOpenScore

                    val expression = classifyExpression(
                        smileScore = smileScore,
                        blinkScore = blinkScore
                    )

                    onResult(
                        FaceExpressionResult(
                            expression = expression,
                            smileScore = smileScore,
                            blinkScore = blinkScore,
                            jawOpenScore = 0f,
                            browDownScore = 0f,
                            faceDetected = true
                        )
                    )
                }
            }
            .addOnFailureListener {
                onResult(
                    FaceExpressionResult(
                        expression = "Analyzer Error",
                        smileScore = 0f,
                        blinkScore = 0f,
                        jawOpenScore = 0f,
                        browDownScore = 0f,
                        faceDetected = false
                    )
                )
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    private fun classifyExpression(
        smileScore: Float,
        blinkScore: Float
    ): String {
        return when {
            smileScore >= 0.55f -> "Happy"
            blinkScore >= 0.55f -> "Blink / Sleepy"
            else -> "Neutral"
        }
    }

    fun close() {
        detector.close()
    }
}