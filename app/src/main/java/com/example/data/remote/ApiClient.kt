package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun executeAction(
        url: String,
        apiKey: String,
        action: String,
        payload: JSONObject
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext Result.failure(Exception("Apps Script Web App URL is not configured. Go to Manager -> Settings."))
        }

        try {
            val root = JSONObject().apply {
                put("action", action)
                put("apiKey", apiKey)
                put("payload", payload)
            }

            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: $bodyString"))
            }

            val json = JSONObject(bodyString)
            val ok = json.optBoolean("ok", false)
            if (ok) {
                Result.success(json.optJSONObject("data") ?: JSONObject())
            } else {
                val errorObj = json.optJSONObject("error")
                val errMsg = errorObj?.optString("message") ?: "Server returned error"
                val errCode = errorObj?.optString("code") ?: "UNKNOWN"
                Result.failure(Exception("[$errCode] $errMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testConnection(url: String, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext Result.failure(Exception("URL cannot be empty"))
        }
        val res = executeAction(url, apiKey, "ping", JSONObject())
        res.map { it.optString("storeName", "Connection Successful!") }
    }
}
