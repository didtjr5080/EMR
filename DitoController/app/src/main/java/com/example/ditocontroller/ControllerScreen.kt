package com.example.ditocontroller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ControllerScreen(
    bluetoothController: BluetoothController
) {
    var deviceName by remember { mutableStateOf("HC-05") }
    var isConnected by remember { mutableStateOf(false) }
    var lastCommand by remember { mutableStateOf("None") }

    val backgroundBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF101828),
            Color(0xFF172554),
            Color(0xFF0F172A)
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(20.dp)
        ) {
            TopBar(
                isConnected = isConnected
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StatusPanel(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight(),
                    deviceName = deviceName,
                    onDeviceNameChange = { deviceName = it },
                    isConnected = isConnected,
                    lastCommand = lastCommand,
                    onConnectClick = {
                        isConnected = bluetoothController.connect(deviceName)
                    },
                    onDisconnectClick = {
                        bluetoothController.close()
                        isConnected = false
                        lastCommand = "Disconnected"
                    }
                )

                ControlPanel(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                    enabled = isConnected,
                    onCommand = { command ->
                        val success = bluetoothController.sendCommand(command)
                        lastCommand = if (success) command.toString() else "Failed"
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    isConnected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Romi Controller",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "YOLO-Based Interactive Companion Robot",
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = if (isConnected) "● Connected" else "● Disconnected",
            color = if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatusPanel(
    modifier: Modifier,
    deviceName: String,
    onDeviceNameChange: (String) -> Unit,
    isConnected: Boolean,
    lastCommand: String,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Robot Status",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                InfoText("Robot", "Romi")
                InfoText("Mode", "Manual Control")
                InfoText("Bluetooth", if (isConnected) "Connected" else "Disconnected")
                InfoText("Last Command", lastCommand)
            }

            Column {
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = onDeviceNameChange,
                    label = {
                        Text("Bluetooth Device Name")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = if (isConnected) onDisconnectClick else onConnectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color(0xFFEF4444) else Color(0xFF2563EB)
                    )
                ) {
                    Text(
                        text = if (isConnected) "Disconnect" else "Connect",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoText(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = value,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ControlPanel(
    modifier: Modifier,
    enabled: Boolean,
    onCommand: (Char) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.10f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ControlButton(
                    text = "▲",
                    label = "Forward",
                    enabled = enabled,
                    command = 'F',
                    onCommand = onCommand
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ControlButton(
                        text = "◀",
                        label = "Left",
                        enabled = enabled,
                        command = 'L',
                        onCommand = onCommand
                    )

                    StopButton(
                        enabled = enabled,
                        onClick = {
                            onCommand('S')
                        }
                    )

                    ControlButton(
                        text = "▶",
                        label = "Right",
                        enabled = enabled,
                        command = 'R',
                        onCommand = onCommand
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                ControlButton(
                    text = "▼",
                    label = "Backward",
                    enabled = enabled,
                    command = 'B',
                    onCommand = onCommand
                )
            }
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    label: String,
    enabled: Boolean,
    command: Char,
    onCommand: (Char) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                onCommand(command)
            },
            enabled = enabled,
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38BDF8),
                disabledContainerColor = Color(0xFF475569)
            )
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            color = Color(0xFFCBD5E1),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun StopButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(112.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444),
                disabledContainerColor = Color(0xFF475569)
            )
        ) {
            Text(
                text = "■",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Stop",
            color = Color(0xFFCBD5E1),
            style = MaterialTheme.typography.bodySmall
        )
    }
}