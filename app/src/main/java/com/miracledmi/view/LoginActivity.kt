package com.miracledmi.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.miracledmi.R
import com.miracledmi.config.Loading
import com.miracledmi.config.MD5
import com.miracledmi.controller.DogeController
import com.miracledmi.controller.WebController
import com.miracledmi.model.Config
import com.miracledmi.model.User
import org.json.JSONObject
import java.lang.Exception
import java.util.*
import kotlin.concurrent.schedule

class LoginActivity : AppCompatActivity() {
  private lateinit var goTo: Intent
  private lateinit var loading: Loading
  private lateinit var user: User
  private lateinit var response: JSONObject
  private lateinit var config: Config

  private lateinit var username: EditText
  private lateinit var password: EditText
  private lateinit var login: Button
  private lateinit var version: TextView
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login)

    loading = Loading(this)
    user = User(this)
    config = Config(this)

    login = findViewById(R.id.buttonLogin)
    username = findViewById(R.id.editTextUsername)
    password = findViewById(R.id.editTextPassword)
    version = findViewById(R.id.textViewVersion)

    config.setBoolean("chart", true)

    doRequestPermission()

    loading.openDialog()

    val lock = try {
      intent.getSerializableExtra("lock").toString().toBoolean()
    } catch (e: Exception) {
      false
    }

    val versionResponse = try {
      intent.getSerializableExtra("version").toString()
    } catch (e: Exception) {
      "Build Version 0.1"
    }

    version.text = versionResponse

    if (lock) {
      username.isEnabled = false
      password.isEnabled = false
      login.visibility = Button.GONE
    }

    login.setOnClickListener {
      loading.openDialog()
      when {
        username.text.isEmpty() -> {
          Toast.makeText(this, "Your username cannot be empty", Toast.LENGTH_SHORT).show()
          loading.closeDialog()
        }
        password.text.isEmpty() -> {
          Toast.makeText(this, "Your password cannot be empty", Toast.LENGTH_SHORT).show()
          loading.closeDialog()
        }
        else -> {
          loginWeb(username.text.toString(), password.text.toString())
        }
      }
    }

    loading.closeDialog()
  }

  private fun loginWeb(username: String, password: String) {
    loading.openDialog()
    val body = HashMap<String, String>()
    body["a"] = "LoginSession"
    body["username"] = username
    body["password"] = password
    body["ref"] = MD5().convert(username + password + "b0d0nk111179")
    Timer().schedule(1000) {
      response = WebController(body).execute().get()
      if (response["code"] == 200) {
        if (response.getJSONObject("data")["Status"] == "0") {
          runOnUiThread {
            user.setString("usernameWeb", username)
            user.setString("wallet", response.getJSONObject("data")["walletdepo"].toString())
            user.setString("walletWithdraw", response.getJSONObject("data")["walletwdall"].toString())
            user.setString("limitDeposit", response.getJSONObject("data")["maxdepo"].toString())
            user.setString("username", response.getJSONObject("data")["userdoge"].toString())
            user.setString("password", response.getJSONObject("data")["passdoge"].toString())
            loginDoge(user.getString("username"), user.getString("password"))
          }
        } else {
          runOnUiThread {
            Toast.makeText(applicationContext, response["data"].toString(), Toast.LENGTH_SHORT).show()
            loading.closeDialog()
          }
        }
      } else {
        runOnUiThread {
          try {
            Toast.makeText(applicationContext, response.getJSONObject("data")["Pesan"].toString(), Toast.LENGTH_SHORT).show()
          } catch (e: Exception) {
            Toast.makeText(applicationContext, response["data"].toString(), Toast.LENGTH_SHORT).show()
          }
          loading.closeDialog()
        }
      }
    }
  }

  private fun loginDoge(username: String, password: String) {
    loading.openDialog()
    val body = HashMap<String, String>()
    body["a"] = "Login"
    body["key"] = "56f1816842b340a6bc07246801552702"
    body["username"] = username
    body["password"] = password
    body["Totp"] = "''"
    Timer().schedule(1000) {
      response = DogeController(body).execute().get()
      if (response["code"] == 200) {
        runOnUiThread {
          user.setString("key", response.getJSONObject("data")["SessionCookie"].toString())
          goTo = Intent(applicationContext, HomeActivity::class.java)
          startActivity(goTo)
          finish()
          loading.closeDialog()
        }
      }
      else {
        runOnUiThread {
          Toast.makeText(applicationContext, response["data"].toString(), Toast.LENGTH_SHORT).show()
          loading.closeDialog()
        }
      }
    }
  }

  private fun doRequestPermission() {
    if (
      ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
      || ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
      || ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WAKE_LOCK
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      requestPermissions(
        arrayOf(
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WAKE_LOCK
        ), 100
      )
    }
  }
}
