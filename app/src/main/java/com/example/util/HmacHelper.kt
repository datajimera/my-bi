package com.example.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacHelper {
    fun generateReceiptSignature(billId: String, secretKey: String): String {
        return try {
            val sha256Hmac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
            sha256Hmac.init(secretKeySpec)
            val hashBytes = sha256Hmac.doFinal(billId.toByteArray(StandardCharsets.UTF_8))
            bytesToHex(hashBytes).take(16) // 16-char clean secure token
        } catch (e: Exception) {
            // Fallback digest
            val md = MessageDigest.getInstance("SHA-256")
            val fallbackBytes = md.digest((billId + secretKey).toByteArray(StandardCharsets.UTF_8))
            bytesToHex(fallbackBytes).take(16)
        }
    }

    fun buildReceiptQrContent(billId: String, timestamp: Long, secretKey: String): String {
        val sig = generateReceiptSignature(billId, secretKey)
        return "$billId|$timestamp|$sig"
    }

    fun verifyReceiptQr(qrContent: String, secretKey: String): Pair<Boolean, String> {
        val parts = qrContent.split("|")
        if (parts.size < 3) {
            // Check if it's just a raw billId
            if (qrContent.startsWith("C") && qrContent.contains("-")) {
                return Pair(true, qrContent.trim())
            }
            return Pair(false, "")
        }
        val billId = parts[0].trim()
        val signature = parts[2].trim()
        val expectedSig = generateReceiptSignature(billId, secretKey)
        val isValid = signature.equals(expectedSig, ignoreCase = true)
        return Pair(isValid, billId)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
