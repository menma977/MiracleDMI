package com.miracledmi.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.miracledmi.R
import com.miracledmi.config.Loading
import com.miracledmi.config.MD5
import com.miracledmi.config.ValueFormat
import com.miracledmi.controller.WebController
import com.miracledmi.model.User
import org.json.JSONObject
import java.lang.Exception
import java.math.BigDecimal
import java.util.*
import kotlin.concurrent.schedule

class ResultActivity : AppCompatActivity() {
  private lateinit var user: User
  private lateinit var loading: Loading
  private lateinit var status: TextView
  private lateinit var valueFormat: ValueFormat

  private lateinit var uniqueCode: String
  private lateinit var startBalance: BigDecimal
  private lateinit var response: JSONObject
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_result)

    user = User(this)
    loading = Loading(this)
    valueFormat = ValueFormat()

    status = findViewById(R.id.textViewStatus)

    loading.openDialog()

    uniqueCode = intent.getSerializableExtra("uniqueCode").toString()
    startBalance = intent.getSerializableExtra("startBalance").toString().toBigDecimal()

    Timer().schedule(1000) {
      val body = HashMap<String, String>()
      body["a"] = "EndTrading1"
      body["usertrade"] = user.getString("username")
      body["passwordtrade"] = user.getString("password")
      body["notrx"] = intent.getSerializableExtra("uniqueCode").toString()
      body["status"] = intent.getSerializableExtra("status").toString()
      body["startbalance"] = valueFormat.decimalToDoge(startBalance).toPlainString()
      body["ref"] = MD5().convert(
        user.getString("username") +
            user.getString("password") +
            body["notrx"] +
            body["status"] +
            "balanceakhirb0d0nk111179"
      )
      response = WebController(body).execute().get()
      try {
        if (response["code"] == 200) {
          runOnUiThread {
            User(applicationContext).setString("fakeBalance", "0")
            status.text = response.getJSONObject("data")["profit"].toString()
            loading.closeDialog()
          }
        } else {
          runOnUiThread {
            status.text = response["data"].toString()
            loading.closeDialog()
          }
        }
      } catch (e: Exception) {
        runOnUiThread {
          Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  override fun onBackPressed() {
    super.onBackPressed()
    val goTo = Intent(this, MainActivity::class.java)
    startActivity(goTo)
  }
}
