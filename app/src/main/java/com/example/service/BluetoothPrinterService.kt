package com.example.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterService {
    private val TAG = "PrinterService"
    private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        deviceName: String,
        content: String
    ): Boolean = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext false
        if (!adapter.isEnabled) return@withContext false

        val pairedDevices: Set<BluetoothDevice> = adapter.bondedDevices
        val printer = pairedDevices.find { it.name == deviceName } ?: return@withContext false

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            socket = printer.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect()
            outputStream = socket.outputStream

            // Basic ESC/POS formatting
            val escInit = byteArrayOf(0x1B, 0x40) // Initialize
            val escCenter = byteArrayOf(0x1B, 0x61, 0x01) // Center
            val escLeft = byteArrayOf(0x1B, 0x61, 0x00) // Left
            val escBoldOn = byteArrayOf(0x1B, 0x45, 0x01)
            val escBoldOff = byteArrayOf(0x1B, 0x45, 0x00)

            outputStream.write(escInit)
            outputStream.write(escCenter)
            outputStream.write(escBoldOn)
            outputStream.write("NETBILL ISP RECEIPT\n".toByteArray())
            outputStream.write(escBoldOff)
            outputStream.write("--------------------------------\n".toByteArray())
            outputStream.write(escLeft)
            outputStream.write(content.toByteArray())
            outputStream.write("\n\n\n".toByteArray()) // Feed paper
            
            outputStream.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Printing failed: ${e.message}")
            false
        } finally {
            outputStream?.close()
            socket?.close()
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<String> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return adapter.bondedDevices.map { it.name }
    }
}
