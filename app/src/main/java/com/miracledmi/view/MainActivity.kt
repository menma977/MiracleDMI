package com.miracledmi.view

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.miracledmi.R
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity() {
  private lateinit var loading: TextView
  private lateinit var timer: Timer
  private lateinit var goTo : Intent

  var countLoading = 0
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    timer = Timer()

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
    Timer().schedule(5000) {
      timer.cancel()
      timer.purge()
      goTo = Intent(applicationContext, LoginActivity::class.java)
      startActivity(goTo)
    }
  }
}
