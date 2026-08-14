package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import javax.net.SocketFactory

/**
 * A lightweight client for the MikroTik RouterOS API protocol.
 * Handles authentication and basic command execution (Enable/Disable users).
 */
class MikroTikApiService {
    private val TAG = "MikroTikApi"

    suspend fun executeCommand(
        host: String,
        port: Int,
        user: String,
        pass: String,
        command: String,
        params: Map<String, String> = emptyMap()
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = SocketFactory.getDefault().createSocket()
            socket.connect(InetSocketAddress(host, port), 5000)
            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            // 1. Login Phase 1
            writeSentence(output, listOf("/login"))
            val response = readSentence(input)
            
            // Check for !done with =ret= (challenge)
            var challenge = ""
            response.forEach { word ->
                if (word.startsWith("=ret=")) {
                    challenge = word.substring(5)
                }
            }

            // 2. Login Phase 2 (Challenge-Response)
            val responseHash = hashPassword(user, pass, challenge)
            writeSentence(output, listOf("/login", "=name=$user", "=response=00$responseHash"))
            val loginResult = readSentence(input)
            
            if (loginResult.any { it.startsWith("!trap") }) {
                return@withContext Result.failure(Exception("Login Failed: ${loginResult.joinToString()}"))
            }

            // 3. Execute Actual Command
            val sentence = mutableListOf(command)
            params.forEach { (k, v) -> sentence.add("=$k=$v") }
            writeSentence(output, sentence)
            
            val cmdResult = readSentence(input)
            Result.success(cmdResult)

        } catch (e: Exception) {
            Log.e(TAG, "MikroTik Connection Error: ${e.message}")
            Result.failure(e)
        } finally {
            socket?.close()
        }
    }

    /**
     * Specifically for ISP automation: Enable or Disable a PPPoE user.
     */
    suspend fun setPppoeUserStatus(
        router: com.example.data.entity.MikroTikRouterEntity,
        pppoeUser: String,
        enable: Boolean
    ): Boolean {
        val cmd = if (enable) "/ppp/secret/enable" else "/ppp/secret/disable"
        // First we find the internal .id of the user
        val findResult = executeCommand(
            router.ipAddress, router.apiPort, router.username, router.password,
            "/ppp/secret/print", mapOf(".proplist" to ".id", "?name" to pppoeUser)
        )

        return findResult.mapCatching { words ->
            var internalId = ""
            words.forEach { if (it.startsWith("=.id=")) internalId = it.substring(5) }
            
            if (internalId.isNotEmpty()) {
                val actionResult = executeCommand(
                    router.ipAddress, router.apiPort, router.username, router.password,
                    cmd, mapOf(".id" to internalId)
                )
                actionResult.isSuccess
            } else false
        }.getOrDefault(false)
    }

    /**
     * Update a user's package (Profile) and MAC binding in MikroTik.
     */
    suspend fun updatePppoeUser(
        router: com.example.data.entity.MikroTikRouterEntity,
        pppoeUser: String,
        profile: String,
        macAddress: String = "",
        staticIp: String = ""
    ): Boolean {
        val findResult = executeCommand(
            router.ipAddress, router.apiPort, router.username, router.password,
            "/ppp/secret/print", mapOf(".proplist" to ".id", "?name" to pppoeUser)
        )

        return findResult.mapCatching { words ->
            var internalId = ""
            words.forEach { if (it.startsWith("=.id=")) internalId = it.substring(5) }
            
            if (internalId.isNotEmpty()) {
                val params = mutableMapOf(".id" to internalId, "profile" to profile)
                if (macAddress.isNotBlank()) params["caller-id"] = macAddress
                if (staticIp.isNotBlank()) params["remote-address"] = staticIp

                val updateResult = executeCommand(
                    router.ipAddress, router.apiPort, router.username, router.password,
                    "/ppp/secret/set", params
                )
                updateResult.isSuccess
            } else {
                // If user doesn't exist, create it
                val params = mutableMapOf("name" to pppoeUser, "password" to "123456", "profile" to profile, "service" to "pppoe")
                if (macAddress.isNotBlank()) params["caller-id"] = macAddress
                if (staticIp.isNotBlank()) params["remote-address"] = staticIp

                val createResult = executeCommand(
                    router.ipAddress, router.apiPort, router.username, router.password,
                    "/ppp/secret/add", params
                )
                createResult.isSuccess
            }
        }.getOrDefault(false)
    }

    /**
     * Fetches current traffic (Rx/Tx) for a specific PPPoE user.
     */
    suspend fun getPppoeUserTraffic(
        router: com.example.data.entity.MikroTikRouterEntity,
        pppoeUser: String
    ): Pair<Double, Double>? {
        val result = executeCommand(
            router.ipAddress, router.apiPort, router.username, router.password,
            "/ppp/active/print", mapOf(".proplist" to "rx-bits-per-second,tx-bits-per-second", "?name" to pppoeUser)
        )

        return result.mapCatching { words ->
            var rx = 0.0
            var tx = 0.0
            words.forEach { word ->
                if (word.startsWith("=rx-bits-per-second=")) rx = word.substring(20).toDoubleOrNull() ?: 0.0
                if (word.startsWith("=tx-bits-per-second=")) tx = word.substring(20).toDoubleOrNull() ?: 0.0
            }
            (rx / 1024 / 1024) to (tx / 1024 / 1024)
        }.getOrNull()
    }

    private fun writeSentence(out: DataOutputStream, sentence: List<String>) {
        sentence.forEach { word ->
            writeWord(out, word)
        }
        out.writeByte(0) // End of sentence
        out.flush()
    }

    private fun writeWord(out: DataOutputStream, word: String) {
        val bytes = word.toByteArray(Charsets.UTF_8)
        writeLength(out, bytes.size)
        out.write(bytes)
    }

    private fun writeLength(out: DataOutputStream, len: Int) {
        if (len < 0x80) {
            out.writeByte(len)
        } else if (len < 0x4000) {
            out.writeByte((len shr 8) or 0x80)
            out.writeByte(len and 0xFF)
        } else if (len < 0x200000) {
            out.writeByte((len shr 16) or 0xC0)
            out.writeByte((len shr 8) and 0xFF)
            out.writeByte(len and 0xFF)
        }
        // Simplified: won't handle words larger than 2MB
    }

    private fun readSentence(input: DataInputStream): List<String> {
        val sentence = mutableListOf<String>()
        while (true) {
            val len = readLength(input)
            if (len == 0) break
            val bytes = ByteArray(len)
            input.readFully(bytes)
            sentence.add(String(bytes, Charsets.UTF_8))
        }
        return sentence
    }

    private fun readLength(input: DataInputStream): Int {
        var first = input.readUnsignedByte()
        if (first and 0x80 == 0) return first
        if (first and 0x40 == 0) return ((first and 0x3F) shl 8) or input.readUnsignedByte()
        if (first and 0x20 == 0) return ((first and 0x1F) shl 16) or (input.readUnsignedByte() shl 8) or input.readUnsignedByte()
        return 0 // Simplified
    }

    private fun hashPassword(user: String, pass: String, challenge: String): String {
        val md = MessageDigest.getInstance("MD5")
        // Protocol: md5( 0 + password + challenge_hex_to_bytes )
        val challengeBytes = challenge.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        md.update(byteArrayOf(0))
        md.update(pass.toByteArray())
        md.update(challengeBytes)
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
