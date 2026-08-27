package com.example.ditocontroller


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.util.UUID

class BluetoothController {

    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val sppUuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun connect(deviceName: String): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return false

            val device = adapter.bondedDevices.firstOrNull {
                it.name == deviceName
            } ?: return false

            bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUuid)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream

            true
        } catch (e: Exception) {
            e.printStackTrace()
            close()
            false
        }
    }

    fun sendCommand(command: Char): Boolean {
        return try {
            outputStream?.write(command.code)
            outputStream?.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun close() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        outputStream = null
        bluetoothSocket = null
    }
}