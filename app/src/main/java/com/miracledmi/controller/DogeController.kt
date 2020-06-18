package com.miracledmi.controller

import android.os.AsyncTask
import com.miracledmi.config.MapToJason
import com.miracledmi.model.Url
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class DogeController(private var body: HashMap<String, String>) : AsyncTask<Void, Void, JSONObject>() {
  override fun doInBackground(vararg params: Void?): JSONObject {
    return try {
      val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
      val mediaType: MediaType = "application/x-www-form-urlencoded".toMediaType()
      val body = MapToJason().map(body).toRequestBody(mediaType)
      val request: Request = Request.Builder().url(Url.doge()).post(body).build()
      val response: Response = client.newCall(request).execute()
      return when {
        response.isSuccessful -> {
          val input = BufferedReader(InputStreamReader(response.body!!.byteStream()))
          val inputData: String = input.readLine()
          val convertJSON = JSONObject(inputData)
          when {
            convertJSON.toString().contains("ChanceTooHigh") -> {
              JSONObject().put("code", 404).put("data", "Chance Too High")
            }
            convertJSON.toString().contains("ChanceTooLow") -> {
              JSONObject().put("code", 404).put("data", "Chance Too Low")
            }
            convertJSON.toString().contains("InsufficientFunds") -> {
              JSONObject().put("code", 404).put("data", "Insufficient Funds")
            }
            convertJSON.toString().contains("NoPossibleProfit") -> {
              JSONObject().put("code", 404).put("data", "No Possible Profit")
            }
            convertJSON.toString().contains("MaxPayoutExceeded") -> {
              JSONObject().put("code", 404).put("data", "Max Payout Exceeded")
            }
            convertJSON.toString().contains("error") -> {
              JSONObject().put("code", 500).put("data", "Invalid request")
            }
            else -> {
              JSONObject().put("code", 200).put("data", convertJSON)
            }
          }
        }
        else -> {
          JSONObject().put("code", 500).put("data", "Unstable connection / Response Not found")
        }
      }
    } catch (e: Exception) {
      JSONObject().put("code", 500).put("data", e.message)
    }
  }
}