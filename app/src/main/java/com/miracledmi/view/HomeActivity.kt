package com.miracledmi.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.miracledmi.R
import com.miracledmi.config.Loading
import com.miracledmi.model.Config
import java.util.*
import kotlin.concurrent.schedule

class HomeActivity : AppCompatActivity() {
  private lateinit var goTo: Intent
  private lateinit var loading: Loading
  private lateinit var config: Config

  private lateinit var copy: Button
  private lateinit var withdrawAll: Button
  private lateinit var activeChart: Switch
  private lateinit var activeProgressBar: Switch

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_home)

    loading = Loading(this)
    config = Config(this)

    copy = findViewById(R.id.buttonCopy)
    withdrawAll = findViewById(R.id.buttonWithdrawAll)
    activeChart = findViewById(R.id.switchActiveChart)
    activeProgressBar = findViewById(R.id.switchActiveProgressBar)

    activeChart.isChecked = config.getBoolean("chart")
    activeProgressBar.isChecked = config.getBoolean("progressBar")

    activeChart.setOnCheckedChangeListener { _, isChecked ->
      config.setBoolean("chart", isChecked)
    }

    activeProgressBar.setOnCheckedChangeListener { _, isChecked ->
      config.setBoolean("progressBar", isChecked)
    }

    withdrawAll.setOnClickListener {
      loading.openDialog()
      Timer().schedule(5000) {
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
