package com.miracledmi.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.miracledmi.R
import com.miracledmi.config.Loading
import java.util.*
import kotlin.concurrent.schedule

class LoginActivity : AppCompatActivity() {
  private lateinit var goTo : Intent
  private lateinit var loading: Loading

  private lateinit var login : Button

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_login)

    loading = Loading(this)

    login = findViewById(R.id.buttonLogin)

    login.setOnClickListener {
      loading.openDialog()
      Timer().schedule(2000) {
        runOnUiThread {
          goTo = Intent(applicationContext, HomeActivity::class.java)
          startActivity(goTo)
          loading.closeDialog()
          finish()
        }
      }
    }
  }
}
