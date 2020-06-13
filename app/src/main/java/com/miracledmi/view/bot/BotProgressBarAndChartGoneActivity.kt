package com.miracledmi.view.bot

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.miracledmi.R
import com.miracledmi.config.Loading
import com.miracledmi.config.ValueFormat
import com.miracledmi.controller.DogeController
import com.miracledmi.model.User
import com.miracledmi.view.ResultActivity
import org.json.JSONObject
import java.math.BigDecimal

class BotProgressBarAndChartGoneActivity : AppCompatActivity() {
  private lateinit var goTo: Intent
  private lateinit var user: User
  private lateinit var loading: Loading
  private lateinit var response: JSONObject
  private lateinit var valueFormat: ValueFormat

  private lateinit var balance: BigDecimal
  private lateinit var balanceTarget: BigDecimal
  private lateinit var balanceRemaining: BigDecimal
  private lateinit var payIn: BigDecimal
  private lateinit var payOut: BigDecimal
  private lateinit var profit: BigDecimal

  private lateinit var balanceView: TextView
  private lateinit var balanceRemainingView: TextView

  private lateinit var uniqueCode: String

  private var loseBot = false
  private var balanceLimitTarget = BigDecimal(0.05)
  private var balanceLimitTargetLow = BigDecimal(0.4)
  private var formula = 1
  private var seed = (0..99999).random().toString()
  private var thread = Thread()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bot_progress_bar_and_chart_gone)

    loading = Loading(this)
    user = User(this)
    valueFormat = ValueFormat()

    uniqueCode = intent.getSerializableExtra("uniqueCode").toString()

    balanceView = findViewById(R.id.textViewBalance)
    balanceRemainingView = findViewById(R.id.textViewRemainingBalance)

    loading.openDialog()
    balance = intent.getSerializableExtra("balance").toString().toBigDecimal()
    balanceRemaining = balance
    balanceTarget = valueFormat.dogeToDecimal(valueFormat.decimalToDoge((balance * balanceLimitTarget) + valueFormat.decimalToDoge(balance)))
    payIn = valueFormat.dogeToDecimal(valueFormat.decimalToDoge(balance) * BigDecimal(0.001))
    balanceLimitTargetLow = valueFormat.dogeToDecimal(valueFormat.decimalToDoge(balance) * BigDecimal(0.4))

    balanceView.text = valueFormat.decimalToDoge(balance).toPlainString()
    balanceRemainingView.text = valueFormat.decimalToDoge(balanceRemaining).toPlainString()

    loading.closeDialog()
    thread = Thread() {
      onBotMode()
    }
    thread.start()
  }

  private fun onBotMode() {
    var time = System.currentTimeMillis()
    val trigger = Object()
    synchronized(trigger) {
      while (balanceRemaining in balanceLimitTargetLow..balanceTarget) {
        val delta = System.currentTimeMillis() - time
        if (delta >= 2500) {
          time = System.currentTimeMillis()
          payIn *= formula.toBigDecimal()
          val body = HashMap<String, String>()
          body["a"] = "PlaceBet"
          body["s"] = user.getString("key")
          body["Low"] = "0"
          body["High"] = "500000"
          body["PayIn"] = payIn.toPlainString()
          body["ProtocolVersion"] = "2"
          body["ClientSeed"] = seed
          body["Currency"] = "doge"
          response = DogeController(body).execute().get()
          if (response["code"] == 200) {
            seed = response.getJSONObject("data")["Next"].toString()
            payOut = response.getJSONObject("data")["PayOut"].toString().toBigDecimal()
            balanceRemaining = response.getJSONObject("data")["StartingBalance"].toString().toBigDecimal()
            profit = payOut - payIn
            balanceRemaining += profit
            loseBot = profit < BigDecimal(0)
            payIn = valueFormat.dogeToDecimal(valueFormat.decimalToDoge(balanceRemaining) * BigDecimal(0.001))

            formula = if (loseBot) {
              2
            } else {
              1
            }

            runOnUiThread {
              balanceRemainingView.text = valueFormat.decimalToDoge(balanceRemaining).toPlainString()
            }
          } else {
            runOnUiThread {
              balanceRemainingView.text = "sleep mode Active"
              Toast.makeText(applicationContext, "sleep mode Active Wait to continue", Toast.LENGTH_LONG).show()
            }
            trigger.wait(60000)
          }
        }
      }
      if (balanceRemaining >= balanceTarget) {
        runOnUiThread {
          goTo = Intent(applicationContext, ResultActivity::class.java)
          goTo.putExtra("status", "WIN")
          goTo.putExtra("startBalance", balance)
          goTo.putExtra("endBalance", balanceRemaining)
          goTo.putExtra("uniqueCode", intent.getSerializableExtra("uniqueCode").toString())
          startActivity(goTo)
          finish()
        }
      } else {
        goTo = Intent(applicationContext, ResultActivity::class.java)
        goTo.putExtra("status", "CUT LOSS")
        goTo.putExtra("startBalance", balance)
        goTo.putExtra("endBalance", balanceRemaining)
        goTo.putExtra("uniqueCode", intent.getSerializableExtra("uniqueCode").toString())
        startActivity(goTo)
        finish()
      }
    }
  }
}
