package com.miracledmi.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.miracledmi.BuildConfig
import com.miracledmi.R
import com.miracledmi.config.MD5
import com.miracledmi.config.ValueFormat
import com.miracledmi.controller.WebController
import com.miracledmi.model.Config
import com.miracledmi.model.User
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.schedule
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
  private lateinit var loading: TextView
  private lateinit var timer: Timer
  private lateinit var goTo: Intent
  private lateinit var user: User
  private lateinit var config: Config
  private lateinit var response: JSONObject
  private lateinit var valueFormat: ValueFormat

  private var countLoading = 0
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    valueFormat = ValueFormat()
    timer = Timer()
    user = User(this)
    config = Config(this)

    loading = findViewById(R.id.textViewLoading)

    timer.schedule(500, 500) {
      runOnUiThread {
        when (countLoading) {
          3 -> {
            countLoading = 0
            loading.text = "Loading..."
          }
          2 -> {
            loading.text = "Loading.."
          }
          1 -> {
            loading.text = "Loading."
          }
          else -> {
            loading.text = "Loading"
          }
        }
        countLoading++
      }
    }
  }

  override fun onStart() {
    super.onStart()
    Timer().schedule(2500) {
      val body = HashMap<String, String>()
      body["a"] = "VersiTrade"
      body["usertrade"] = user.getString("username")
      body["passwordtrade"] = user.getString("password")
      body["ref"] = MD5().convert(user.getString("username") + user.getString("password") + "versi" + "b0d0nk111179")
      response = WebController(body).execute().get()
      if (response["code"] == 200) {
        if (response.getJSONObject("data")["Status"] == "0") {
          if (response.getJSONObject("data")["versiapk"] == BuildConfig.VERSION_CODE.toString()) {
            if (user.getString("username").isEmpty()) {
              runOnUiThread {
                timer.cancel()
                timer.purge()
                goTo = Intent(applicationContext, LoginActivity::class.java)
                goTo.putExtra("lock", false)
                goTo.putExtra("version", "Build Version ${BuildConfig.VERSION_NAME}")
                startActivity(goTo)
                finish()
              }
            } else {
              runOnUiThread {
                timer.cancel()
                timer.purge()
                user.setString("wallet", response.getJSONObject("data")["walletdepo"].toString())
                user.setString("limitDeposit", response.getJSONObject("data")["maxdepo"].toString())
                goTo = Intent(applicationContext, HomeActivity::class.java)
                startActivity(goTo)
                finish()
              }
            }
          } else {
            runOnUiThread {
              timer.cancel()
              timer.purge()
              user.clear()
              config.clear()
              goTo = Intent(Intent.ACTION_VIEW, Uri.parse("https://netizenchar.com/download/miracleDMI.apk"))
              startActivity(goTo)
              finish()
            }
          }
        } else {
          runOnUiThread {
            timer.cancel()
            timer.purge()
            user.clear()
            config.clear()
            goTo = Intent(applicationContext, LoginActivity::class.java)
            goTo.putExtra("lock", false)
            goTo.putExtra("version", "Build Version ${BuildConfig.VERSION_NAME}")
            startActivity(goTo)
            finish()
          }
        }
      } else {
        runOnUiThread {
          timer.cancel()
          timer.purge()
          user.clear()
          config.clear()
          finish()
          Timer().schedule(2500) {
            runOnUiThread {
              exitProcess(0)
            }
          }
        }
      }
    }
  }
}
