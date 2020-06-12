package com.miracledmi.view.bot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.ProgressBar
import android.widget.TextView
import com.miracledmi.R
import com.miracledmi.config.Loading
import com.miracledmi.config.ValueFormat
import com.miracledmi.controller.DogeController
import com.miracledmi.model.User
import org.eazegraph.lib.charts.ValueLineChart
import org.eazegraph.lib.models.ValueLinePoint
import org.eazegraph.lib.models.ValueLineSeries
import org.json.JSONObject
import java.math.BigDecimal

class BotActivity : AppCompatActivity() {
  private lateinit var cubicLineChart: ValueLineChart
  private lateinit var series: ValueLineSeries
  private lateinit var goTo: Intent
  private lateinit var progressBar: ProgressBar
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

  private var rowChart = 0
  private var loseBot = false
  private var balanceLimitTarget = BigDecimal(0.05)
  private var balanceLimitTargetLow = BigDecimal(0.4)
  private var formula = 1
  private var seed = (0..99999).random().toString()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bot)

    loading = Loading(this)
    user = User(this)
    valueFormat = ValueFormat()

    uniqueCode = intent.getSerializableExtra("uniqueCode").toString()

    balanceView = findViewById(R.id.textViewBalance)
    balanceRemainingView = findViewById(R.id.textViewRemainingBalance)
    progressBar = findViewById(R.id.progressBar)
    cubicLineChart = findViewById(R.id.cubicLineChart)
    series = ValueLineSeries()

    loading.openDialog()
    balance = intent.getSerializableExtra("balance").toString().toBigDecimal()
    balanceRemaining = balance
    balanceTarget = valueFormat.dogeToDecimal(valueFormat.decimalToDoge((balance * balanceLimitTarget) + balance))
    payIn = valueFormat.dogeToDecimal(valueFormat.decimalToDoge(balance * BigDecimal(0.001)))
    balanceLimitTargetLow = valueFormat.dogeToDecimal(balance * BigDecimal(0.4))

    println(balance)
    println(balanceRemaining)
    println(balanceTarget)
    println("${payIn} - ${payIn.toPlainString().length}")
    println(balanceLimitTargetLow)

    balanceView.text = valueFormat.decimalToDoge(balance).toPlainString()
    balanceRemainingView.text = valueFormat.decimalToDoge(balanceRemaining).toPlainString()

    progress(balance, balanceRemaining, balanceTarget)
    configChart()
    loading.closeDialog()
    val tared = Thread() {
      onBotMode()
    }
    tared.start()
    //onBotMode()
  }

  private fun onBotMode() {
    var time = System.currentTimeMillis()
    while (balanceRemaining <= balanceTarget || balanceRemaining > balanceLimitTargetLow) {
      val delta = System.currentTimeMillis() - time
      if (delta >= 2500) {
        time = System.currentTimeMillis()
        val body = HashMap<String, String>()
        body["a"] = "PlaceBet"
        body["s"] = user.getString("key")
        body["Low"] = "0"
        body["High"] = "940000"
        body["PayIn"] = (payIn * formula.toBigDecimal()).toPlainString()
        body["ProtocolVersion"] = "2"
        body["ClientSeed"] = seed
        body["Currency"] = "doge"
        println(body)
        response = DogeController(body).execute().get()
        println(response)
        if (response["code"] == 200) {
          seed = response.getJSONObject("data")["Next"].toString()
          payOut = response.getJSONObject("data")["PayOut"].toString().toBigDecimal()
          balanceRemaining = response.getJSONObject("data")["StartingBalance"].toString().toBigDecimal()
          payIn = valueFormat.dogeToDecimal(valueFormat.decimalToDoge(balanceRemaining * BigDecimal(0.001)))
          profit = payOut - payIn
          loseBot = profit < BigDecimal(0)
          println("========================================")
          println("payOut $payOut ${payOut.toString().length}")
          println("balanceRemaining $balanceRemaining ${balanceRemaining.toString().length}")
          println("payIn $payIn ${payIn.toString().length}")
          println("profit $profit ${profit.toString().length}")
          println(loseBot)
          if (loseBot) {
            formula += 19
          } else {
            if (formula == 1) {
              formula = 1
            } else {
              formula -= 1
            }
          }
          runOnUiThread {
            balanceRemainingView.text = valueFormat.decimalToDoge(balanceRemaining).toPlainString()
            progress(balance, balanceRemaining, balanceTarget)
            if (rowChart >= 39) {
              series.series.removeAt(0)
              series.addPoint(ValueLinePoint("$rowChart", valueFormat.decimalToDoge(balanceRemaining).toFloat()))
            } else {
              series.addPoint(ValueLinePoint("$rowChart", valueFormat.decimalToDoge(balanceRemaining).toFloat()))
            }
            cubicLineChart.addSeries(series)
            cubicLineChart.refreshDrawableState()
          }
          rowChart++
          if (rowChart <= 30) {
            println("Proces $rowChart = ${balanceRemaining + profit} = re : $balanceRemaining")
          } else {
            println("hasil : ${balanceRemaining + profit} - ${valueFormat.decimalToDoge(balanceRemaining + profit)}")
            break
          }
        }
      }
    }
  }

  private fun configChart() {
    series.color = getColor(R.color.colorAccent)
    cubicLineChart.axisTextColor = getColor(R.color.textPrimary)
    cubicLineChart.containsPoints()
    cubicLineChart.isUseDynamicScaling = true
    cubicLineChart.addSeries(series)
    cubicLineChart.startAnimation()
  }

  private fun progress(start: BigDecimal, remaining: BigDecimal, end: BigDecimal) {
    println("=============Progress Bar===============")
    println(valueFormat.decimalToDoge(start).setScale(0).toInt())
    println(valueFormat.decimalToDoge(remaining).setScale(0).toInt())
    println(valueFormat.decimalToDoge(end).setScale(0).toInt())
    println("=============Progress Bar===============")
    progressBar.min = valueFormat.decimalToDoge(start).setScale(0).toInt()
    progressBar.progress = valueFormat.decimalToDoge(remaining).setScale(0).toInt()
    progressBar.max = valueFormat.decimalToDoge(end).setScale(0).toInt()
  }
}
