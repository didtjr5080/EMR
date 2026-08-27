package com.example.ditoapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.util.UUID

data class PairedBluetoothDevice(
    val name: String,
    val address: String,
    val device: BluetoothDevice
)

class BluetoothController(
    private val context: Context
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null

    private val sppUuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<PairedBluetoothDevice> {
        val adapter = bluetoothAdapter ?: return emptyList()

        return adapter.bondedDevices.map { device ->
            PairedBluetoothDevice(
                name = device.name ?: "이름 없는 기기",
                address = device.address ?: "주소 없음",
                device = device
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            disconnect()

            val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)

            bluetoothAdapter?.cancelDiscovery()

            newSocket.connect()
            socket = newSocket

            true
        } catch (e: IOException) {
            e.printStackTrace()
            disconnect()
            false
        } catch (e: SecurityException) {
            e.printStackTrace()
            disconnect()
            false
        }
    }

    fun sendCommand(command: String): Boolean {
        return try {
            val currentSocket = socket ?: return false
            val outputStream = currentSocket.outputStream

            outputStream.write(command.toByteArray())
            outputStream.write('\n'.code)
            outputStream.flush()

            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            socket = null
        }
    }
}