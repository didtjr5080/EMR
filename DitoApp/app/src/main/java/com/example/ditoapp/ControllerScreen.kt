package com.example.ditoapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ControllerScreen(
    onMoveToFaceExpression: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bluetoothController = remember {
        BluetoothController(context)
    }

    var isConnected by remember {
        mutableStateOf(false)
    }

    var selectedDevice by remember {
        mutableStateOf<PairedBluetoothDevice?>(null)
    }

    var lastCommand by remember {
        mutableStateOf("None")
    }

    var showDeviceDialog by remember {
        mutableStateOf(false)
    }

    val pairedDevices = remember {
        mutableStateListOf<PairedBluetoothDevice>()
    }

    var hasBluetoothPermission by remember {
        mutableStateOf(checkBluetoothPermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasBluetoothPermission = result.values.all { it }

        if (hasBluetoothPermission) {
            pairedDevices.clear()
            pairedDevices.addAll(bluetoothController.getPairedDevices())
        }
    }

    LaunchedEffect(Unit) {
        if (!hasBluetoothPermission) {
            permissionLauncher.launch(requiredBluetoothPermissions())
        } else {
            pairedDevices.clear()
            pairedDevices.addAll(bluetoothController.getPairedDevices())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            bluetoothController.disconnect()
        }
    }

    fun refreshDevices() {
        if (!hasBluetoothPermission) {
            permissionLauncher.launch(requiredBluetoothPermissions())
            return
        }

        pairedDevices.clear()
        pairedDevices.addAll(bluetoothController.getPairedDevices())
    }

    fun connectSelectedDevice() {
        val device = selectedDevice ?: return

        scope.launch {
            val connected = withContext(Dispatchers.IO) {
                bluetoothController.connect(device.device)
            }

            isConnected = connected
        }
    }

    fun disconnectDevice() {
        bluetoothController.disconnect()
        isConnected = false
    }

    fun sendCommand(command: String) {
        lastCommand = command

        scope.launch {
            withContext(Dispatchers.IO) {
                bluetoothController.sendCommand(command)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF071327),
                        Color(0xFF102A63),
                        Color(0xFF091124)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(
                start = 22.dp,
                end = 22.dp,
                top = 18.dp,
                bottom = 18.dp
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            RobotStatusCard(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight(),
                isConnected = isConnected,
                selectedDevice = selectedDevice,
                lastCommand = lastCommand,
                onDeviceClick = {
                    refreshDevices()
                    showDeviceDialog = true
                },
                onConnectClick = {
                    if (isConnected) {
                        disconnectDevice()
                    } else {
                        connectSelectedDevice()
                    }
                },
                onMoveToFaceExpression = onMoveToFaceExpression
            )

            Spacer(modifier = Modifier.width(18.dp))

            RightControlArea(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight(),
                onCommand = { command ->
                    sendCommand(command)
                }
            )
        }
    }

    if (showDeviceDialog) {
        DeviceSelectDialog(
            devices = pairedDevices,
            onDismiss = {
                showDeviceDialog = false
            },
            onSelect = { device ->
                selectedDevice = device
                showDeviceDialog = false
            }
        )
    }
}

@Composable
private fun RobotStatusCard(
    modifier: Modifier,
    isConnected: Boolean,
    selectedDevice: PairedBluetoothDevice?,
    lastCommand: String,
    onDeviceClick: () -> Unit,
    onConnectClick: () -> Unit,
    onMoveToFaceExpression: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C3E63).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 24.dp,
                    bottom = 24.dp
                )
        ) {
            Text(
                text = "Robot Status",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            StatusText(
                label = "Robot",
                value = "Dito"
            )

            StatusText(
                label = "Mode",
                value = "Manual"
            )

            StatusText(
                label = "Bluetooth",
                value = if (isConnected) "Connected" else "Disconnected"
            )

            StatusText(
                label = "Device",
                value = selectedDevice?.name ?: "Not selected"
            )

            StatusText(
                label = "Last",
                value = lastCommand
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDeviceClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF735CFF),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Device",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2F6EFF),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isConnected) "Disconnect" else "Connect",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onMoveToFaceExpression,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6B56F6),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "얼굴 표정 분석",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Face Expression Analysis",
                color = Color(0xFFC8D0E0),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun StatusText(
    label: String,
    value: String
) {
    Text(
        text = label,
        color = Color(0xFFB8C2D6),
        fontSize = 18.sp
    )

    Text(
        text = value,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(11.dp))
}

@Composable
private fun RightControlArea(
    modifier: Modifier,
    onCommand: (String) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF33466E).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 34.dp,
                    vertical = 24.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            DirectionPad(
                onCommand = onCommand
            )
        }
    }
}

@Composable
private fun DirectionPad(
    onCommand: (String) -> Unit
) {
    val mainButtonWidth = 98.dp
    val sideButtonWidth = 98.dp
    val stopButtonWidth = 98.dp
    val buttonHeight = 50.dp

    val horizontalGap = 34.dp
    val verticalGap = 18.dp

    Column(
        modifier = Modifier.wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ControllerButton(
            symbol = "▲",
            label = "Forward",
            width = mainButtonWidth,
            height = buttonHeight,
            onClick = {
                onCommand("FORWARD")
            }
        )

        Spacer(modifier = Modifier.height(verticalGap))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            ControllerButton(
                symbol = "◀",
                label = "Left",
                width = sideButtonWidth,
                height = buttonHeight,
                onClick = {
                    onCommand("LEFT")
                }
            )

            Spacer(modifier = Modifier.width(horizontalGap))

            ControllerButton(
                symbol = "■",
                label = "Stop",
                width = stopButtonWidth,
                height = buttonHeight,
                onClick = {
                    onCommand("STOP")
                }
            )

            Spacer(modifier = Modifier.width(horizontalGap))

            ControllerButton(
                symbol = "▶",
                label = "Right",
                width = sideButtonWidth,
                height = buttonHeight,
                onClick = {
                    onCommand("RIGHT")
                }
            )
        }

        Spacer(modifier = Modifier.height(verticalGap))

        ControllerButton(
            symbol = "▼",
            label = "Backward",
            width = mainButtonWidth,
            height = buttonHeight,
            onClick = {
                onCommand("BACKWARD")
            }
        )
    }
}

@Composable
private fun ControllerButton(
    symbol: String,
    label: String,
    width: Dp,
    height: Dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .width(width)
                .height(height),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5B6D91).copy(alpha = 0.94f),
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = symbol,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = label,
            color = Color(0xFFD6DCEA),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DeviceSelectDialog(
    devices: List<PairedBluetoothDevice>,
    onDismiss: () -> Unit,
    onSelect: (PairedBluetoothDevice) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("페어링된 기기 선택")
        },
        text = {
            Column {
                if (devices.isEmpty()) {
                    Text("페어링된 Bluetooth 기기가 없습니다.")
                } else {
                    devices.forEach { device ->
                        TextButton(
                            onClick = {
                                onSelect(device)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${device.name} / ${device.address}")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("닫기")
            }
        }
    )
}

private fun requiredBluetoothPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }
}

private fun checkBluetoothPermissions(context: Context): Boolean {
    return requiredBluetoothPermissions().all { permission ->
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}