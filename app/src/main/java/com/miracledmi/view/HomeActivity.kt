package com.miracledmi.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.miracledmi.R
import com.miracledmi.config.Loading
import com.miracledmi.config.ValueFormat
import com.miracledmi.controller.DogeController
import com.miracledmi.model.Config
import com.miracledmi.model.User
import com.miracledmi.view.bot.BotActivity
import org.json.JSONObject
import java.math.BigDecimal
import java.util.*
import kotlin.concurrent.schedule

class HomeActivity : AppCompatActivity() {
  private lateinit var goTo: Intent
  private lateinit var loading: Loading
  private lateinit var config: Config
  private lateinit var user: User
  private lateinit var response: JSONObject
  private lateinit var balanceValue: BigDecimal
  private lateinit var valueFormat: ValueFormat

  private lateinit var copy: Button
  private lateinit var withdrawAll: Button
  private lateinit var activeChart: Switch
  private lateinit var activeProgressBar: Switch
  private lateinit var play: Button
  private lateinit var logout: Button
  private lateinit var wallet: TextView
  private lateinit var balance: TextView
  private lateinit var refreshBalance: LinearLayout
  private lateinit var withdrawContent: LinearLayout
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_home)

    loading = Loading(this)
    config = Config(this)
    user = User(this)
    valueFormat = ValueFormat()

    copy = findViewById(R.id.buttonCopy)
    withdrawAll = findViewById(R.id.buttonWithdrawAll)
    activeChart = findViewById(R.id.switchActiveChart)
    activeProgressBar = findViewById(R.id.switchActiveProgressBar)
    play = findViewById(R.id.buttonPlay)
    logout = findViewById(R.id.buttonLogout)
    wallet = findViewById(R.id.textViewWallet)
    balance = findViewById(R.id.textViewBalance)
    refreshBalance = findViewById(R.id.linearLayoutRefreshBalance)
    withdrawContent = findViewById(R.id.linearLayoutWithdrawContent)

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
          finish()
          loading.closeDialog()
        }
      }
    }

    play.setOnClickListener {
      loading.openDialog()
      Timer().schedule(5000) {
        runOnUiThread {
          goTo = Intent(applicationContext, BotActivity::class.java)
          startActivity(goTo)
          finish()
          loading.closeDialog()
        }
      }
    }

    refreshBalance.setOnClickListener {
      loading.openDialog()
      getBalance()
    }

    logout.setOnClickListener {
      loading.openDialog()
      user.clear()
      Timer().schedule(100) {
        runOnUiThread {
          goTo = Intent(applicationContext, MainActivity::class.java)
          startActivity(goTo)
          finish()
          loading.closeDialog()
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    loading.openDialog()
    getBalance()
  }

  private fun getBalance() {
    val body = HashMap<String, String>()
    body["a"] = "GetBalance"
    body["s"] = user.getString("key")
    body["Currency"] = "doge"
    body["Referrals"] = "0"
    body["Stats"] = "0"
    Timer().schedule(2500) {
      response = DogeController(body).execute().get()
      if (response["code"] == 200) {
        balanceValue = response.getJSONObject("data")["Balance"].toString().toBigDecimal()
        val balanceLimit = valueFormat.bigDecimalToDoge(user.getString("limitDeposit").toBigDecimal())
        if (balanceValue > BigDecimal(0) && balanceValue < balanceLimit) {
          runOnUiThread {
            withdrawContent.visibility = LinearLayout.GONE
            play.isEnabled = true
            balance.text = "${valueFormat.dogeToBigDecimal(balanceValue).toPlainString()} DOGE"
            loading.closeDialog()
          }
        } else if (balanceValue >= balanceLimit) {
          runOnUiThread {
            withdrawContent.visibility = LinearLayout.VISIBLE
            play.isEnabled = false
            balance.text = "${valueFormat.dogeToBigDecimal(balanceValue).toPlainString()} DOGE"
            loading.closeDialog()
          }
        } else {
          runOnUiThread {
            withdrawContent.visibility = LinearLayout.GONE
            play.isEnabled = false
            balance.text = "${valueFormat.dogeToBigDecimal(balanceValue).toPlainString()} Doge too small"
            loading.closeDialog()
          }
        }
      } else {
        runOnUiThread {
          withdrawContent.visibility = LinearLayout.GONE
          play.isEnabled = false
          balance.text = response["data"].toString()
          loading.closeDialog()
        }
      }
    }
  }
}
