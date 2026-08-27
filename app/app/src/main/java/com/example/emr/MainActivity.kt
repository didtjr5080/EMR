package com.example.emr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var responsibleCodeInput: EditText
    private lateinit var scanQrButton: Button
    private lateinit var stopScanButton: Button
    private lateinit var loadMyTasksButton: Button
    private lateinit var previewView: PreviewView
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var adapter: TaskAdapter

    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    private var taskListener: ListenerRegistration? = null
    private var currentResponsibleCode: String = ""
    private var currentResponsibleName: String = "unknown"

    private var isScanning = false
    private var isProcessingFrame = false
    private var lastEmptyCodeToastTime = 0L

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startFrontCameraQrScan()
            } else {
                statusText.text = "카메라 권한이 거부되었습니다."
                Toast.makeText(this, "QR 스캔을 위해 카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        responsibleCodeInput = findViewById(R.id.responsibleCodeInput)
        scanQrButton = findViewById(R.id.scanQrButton)
        stopScanButton = findViewById(R.id.stopScanButton)
        loadMyTasksButton = findViewById(R.id.loadMyTasksButton)
        previewView = findViewById(R.id.previewView)
        taskRecyclerView = findViewById(R.id.taskRecyclerView)

        adapter = TaskAdapter { task ->
            completeTask(task)
        }

        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        taskRecyclerView.adapter = adapter

        cameraExecutor = Executors.newSingleThreadExecutor()

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8
            )
            .build()

        barcodeScanner = BarcodeScanning.getClient(options)

        scanQrButton.setOnClickListener {
            ensureCameraPermissionAndStartScan()
        }

        stopScanButton.setOnClickListener {
            stopQrScan()
        }

        loadMyTasksButton.setOnClickListener {
            val code = responsibleCodeInput.text.toString().trim()
            loadTasksForResponsibleCode(code)
        }

        responsibleCodeInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val code = responsibleCodeInput.text.toString().trim()
                loadTasksForResponsibleCode(code)
                true
            } else {
                false
            }
        }

        statusText.text = "담당자 코드를 입력하거나 전면 카메라로 QR을 스캔하세요."
    }

    override fun onDestroy() {
        super.onDestroy()
        taskListener?.remove()

        if (::barcodeScanner.isInitialized) {
            barcodeScanner.close()
        }

        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    private fun ensureCameraPermissionAndStartScan() {
        val permissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            startFrontCameraQrScan()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startFrontCameraQrScan() {
        isScanning = true
        isProcessingFrame = false

        previewView.visibility = View.VISIBLE
        stopScanButton.visibility = View.VISIBLE
        scanQrButton.isEnabled = false

        statusText.text = "전면 카메라로 QR/바코드를 스캔 중입니다."

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (exception: Exception) {
                statusText.text = "전면 카메라 실행 실패: ${exception.message}"
                Toast.makeText(
                    this,
                    "전면 카메라를 사용할 수 없습니다.",
                    Toast.LENGTH_LONG
                ).show()
                stopQrScan()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopQrScan() {
        isScanning = false
        isProcessingFrame = false

        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        } catch (_: Exception) {
        }

        previewView.visibility = View.GONE
        stopScanButton.visibility = View.GONE
        scanQrButton.isEnabled = true

        statusText.text = "QR/바코드 스캔이 중지되었습니다."
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: androidx.camera.core.ImageProxy) {
        if (!isScanning || isProcessingFrame) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image

        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        isProcessingFrame = true

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val rawValue = barcodes
                    .firstOrNull { !it.rawValue.isNullOrBlank() }
                    ?.rawValue
                    ?.trim()
                    .orEmpty()

                if (rawValue.isNotBlank()) {
                    runOnUiThread {
                        handleScannedResponsibleCode(rawValue)
                    }
                }
            }
            .addOnFailureListener { exception ->
                runOnUiThread {
                    statusText.text = "QR 분석 실패: ${exception.message}"
                }
            }
            .addOnCompleteListener {
                isProcessingFrame = false
                imageProxy.close()
            }
    }

    private fun handleScannedResponsibleCode(code: String) {
        if (!isScanning) return

        responsibleCodeInput.setText(code)
        responsibleCodeInput.setSelection(code.length)

        Toast.makeText(
            this,
            "담당자 코드 인식: $code",
            Toast.LENGTH_SHORT
        ).show()

        stopQrScan()
        loadTasksForResponsibleCode(code)
    }

    private fun loadTasksForResponsibleCode(code: String) {
        if (code.isBlank()) {
            val now = System.currentTimeMillis()
            if (now - lastEmptyCodeToastTime > 1500) {
                Toast.makeText(
                    this,
                    "담당자 코드를 입력하거나 QR을 스캔하세요.",
                    Toast.LENGTH_SHORT
                ).show()
                lastEmptyCodeToastTime = now
            }

            statusText.text = "담당자 코드가 비어 있습니다."
            return
        }

        currentResponsibleCode = code
        currentResponsibleName = code

        listenPendingTasksByResponsible(code)
    }

    private fun listenPendingTasksByResponsible(responsibleCode: String) {
        taskListener?.remove()

        statusText.text = "담당자 코드 [$responsibleCode] 업무 조회 중..."

        taskListener = db.collection("tasks")
            .whereEqualTo("status", "pending")
            .whereEqualTo("assigned_to_code", responsibleCode)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    statusText.text = "업무 목록 불러오기 실패: ${error.message}"

                    Toast.makeText(
                        this,
                        "Firestore 오류: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addSnapshotListener
                }

                val tasks = snapshots
                    ?.documents
                    ?.mapNotNull { document ->
                        val item = document.toObject(TaskItem::class.java)
                        item?.copy(id = document.id)
                    }
                    ?.sortedWith(
                        compareBy<TaskItem> {
                            it.room_number
                        }.thenBy {
                            it.priority.toIntOrNull() ?: 999
                        }
                    )
                    ?: emptyList()

                adapter.submitList(tasks)

                statusText.text = if (tasks.isEmpty()) {
                    "[$responsibleCode] 미완료 업무가 없습니다."
                } else {
                    "[$responsibleCode] 미완료 업무 ${tasks.size}개"
                }
            }
    }

    private fun completeTask(task: TaskItem) {
        if (task.id.isBlank()) {
            Toast.makeText(this, "업무 ID가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentResponsibleCode.isBlank()) {
            Toast.makeText(this, "담당자 코드를 먼저 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (task.assigned_to_code.isNotBlank() && task.assigned_to_code != currentResponsibleCode) {
            Toast.makeText(this, "현재 담당자의 업무가 아닙니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val taskRef = db.collection("tasks").document(task.id)
        val logRef = db.collection("task_logs").document()

        db.runTransaction { transaction ->
            val snapshot = transaction.get(taskRef)
            val currentStatus = snapshot.getString("status") ?: "pending"
            val assignedCode = snapshot.getString("assigned_to_code") ?: ""

            if (currentStatus == "completed") {
                return@runTransaction null
            }

            if (assignedCode.isNotBlank() && assignedCode != currentResponsibleCode) {
                throw IllegalStateException("현재 담당자와 업무 담당자가 일치하지 않습니다.")
            }

            val completedAtKst = getKstNowIso()
            val deviceId = android.os.Build.MODEL ?: "android"

            transaction.update(
                taskRef,
                mapOf(
                    "status" to "completed",
                    "completed_at" to FieldValue.serverTimestamp(),
                    "completed_at_kst" to completedAtKst,
                    "updated_at" to FieldValue.serverTimestamp(),
                    "updated_at_kst" to completedAtKst,
                    "completed_by" to currentResponsibleName,
                    "completed_by_code" to currentResponsibleCode,
                    "completed_device_id" to deviceId
                )
            )

            transaction.set(
                logRef,
                mapOf(
                    "id" to logRef.id,
                    "task_id" to task.id,

                    "patient_id" to task.patient_id,
                    "patient_name" to task.patient_name,
                    "room_number" to task.room_number,
                    "patient_code" to task.patient_code,

                    "task_name" to task.task_name,
                    "description" to task.description,
                    "scheduled_time" to task.scheduled_time,
                    "priority" to task.priority,

                    "assigned_to" to task.assigned_to,
                    "assigned_to_code" to task.assigned_to_code,

                    "action" to "complete",
                    "completed_at" to FieldValue.serverTimestamp(),
                    "completed_at_kst" to completedAtKst,
                    "completed_by" to currentResponsibleName,
                    "completed_by_code" to currentResponsibleCode,
                    "device_id" to deviceId,
                    "note" to ""
                )
            )

            null
        }.addOnSuccessListener {
            Toast.makeText(
                this,
                "처치완료 시간이 기록되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }.addOnFailureListener { exception ->
            Toast.makeText(
                this,
                "처치완료 기록 실패: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getKstNowIso(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        return Instant.now()
            .atZone(ZoneId.of("Asia/Seoul"))
            .format(formatter)
    }
}