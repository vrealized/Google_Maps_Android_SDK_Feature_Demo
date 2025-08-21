package com.example.googlemapsdemos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.googlemapsdemos.databinding.FactCheckDemoBinding
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class FactCheckDemoActivity : AppCompatActivity() {

    private lateinit var binding: FactCheckDemoBinding

    // 已应用超时设置
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private lateinit var defaultQueryText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FactCheckDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        defaultQueryText = getString(R.string.fact_check_default_query)
        binding.editTextQuery.setText(defaultQueryText)

        binding.editTextQuery.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && binding.editTextQuery.text.toString() == defaultQueryText) {
                binding.editTextQuery.setText("")
            } else if (!hasFocus && binding.editTextQuery.text.toString().isBlank()) {
                binding.editTextQuery.setText(defaultQueryText)
            }
        }

        binding.buttonSubmit.setOnClickListener {
            val query = binding.editTextQuery.text.toString()
            if (query.isNotBlank() && query != defaultQueryText) {
                getFactCheckResult(query)
            } else if (query == defaultQueryText) {
                getFactCheckResult(defaultQueryText)
            } else {
                Toast.makeText(this, "请输入您想核查的问题", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFactCheckResult(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.resultCard.visibility = View.GONE
        binding.textViewResult.text = ""

        lifecycleScope.launch {
            var responseJson: String? = null
            try {
                responseJson = withContext(Dispatchers.IO) {
                    callGoogleAiStudioApi(query)
                }

                binding.progressBar.visibility = View.GONE
                if (responseJson != null) {
                    val resultText = parseGeminiResponse(responseJson)
                    if (resultText.startsWith("错误:")) {
                        Toast.makeText(this@FactCheckDemoActivity, resultText, Toast.LENGTH_LONG).show()
                    } else {
                        binding.textViewResult.text = resultText
                        binding.resultCard.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(this@FactCheckDemoActivity, "未能从服务器获取响应", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                val errorMessage = "请求失败: ${e.message}"
                Toast.makeText(this@FactCheckDemoActivity, errorMessage, Toast.LENGTH_LONG).show()
                Log.e("FactCheckError", "API call failed", e)
                responseJson?.let { Log.e("FactCheckError", "Raw response on error: $it") }
            }
        }
    }

    private fun callGoogleAiStudioApi(query: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank()) {
            throw IllegalStateException("请在 local.properties 文件中配置有效的 GEMINI_API_KEY。")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

        val requestPayload = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(
                    Part("你是一个严谨的事实核查员。你的任务是基于科学和普遍共识来判断用户提供的信息的真伪。请详细解释背后的原理，并明确给出结论（例如：'结论：这个说法是部分正确的' 或 '结论：这个说法是错误的'）。请使用中文回答。"),
                    Part(query)
                ))
            )
        )
        val requestBodyJson = gson.toJson(requestPayload)

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        return response.body?.string()
    }

    private fun parseGeminiResponse(json: String): String {
        return try {
            val response = gson.fromJson(json, GeminiResponse::class.java)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: run {
                    val errorResponse = gson.fromJson(json, ApiErrorResponse::class.java)
                    "错误: ${errorResponse.error?.message ?: "未能从API响应中提取有效内容或错误信息。"}"
                }
        } catch (e: JsonSyntaxException) {
            "错误: 无法解析API响应 (${e.message})。\n原始数据: $json"
        }
    }
}

// --- Data Classes  ---

data class GeminiRequest(val contents: List<Content>)
data class Content(val parts: List<Part>)
data class Part(val text: String)

data class GeminiResponse(val candidates: List<Candidate>?)
data class Candidate(val content: Content?)

data class ApiErrorResponse(val error: ApiError?)
data class ApiError(val message: String?, val code: Int?, val status: String?)