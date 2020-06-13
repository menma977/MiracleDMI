package com.miracledmi.view

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.miracledmi.R
import com.miracledmi.config.Loading
import com.miracledmi.config.MD5
import com.miracledmi.config.ValueFormat
import com.miracledmi.controller.DogeController
import com.miracledmi.controller.WebController
import com.miracledmi.model.Config
import com.miracledmi.model.User
import com.miracledmi.view.bot.BotActivity
import com.miracledmi.view.bot.BotChartGoneActivity
import com.miracledmi.view.bot.BotProgressBarAndChartGoneActivity
import com.miracledmi.view.bot.BotProgressBarGoneActivity
import org.json.JSONObject
import java.math.BigDecimal
import java.math.MathContext
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
  private lateinit var username: TextView
  private lateinit var maxDeposit: TextView
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
    username = findViewById(R.id.textViewUsername)
    maxDeposit = findViewById(R.id.textViewMaxDeposit)
    balance = findViewById(R.id.textViewBalance)
    refreshBalance = findViewById(R.id.linearLayoutRefreshBalance)
    withdrawContent = findViewById(R.id.linearLayoutWithdrawContent)

    wallet.text = user.getString("wallet")
    username.text = user.getString("usernameWeb")
    maxDeposit.text = "${user.getString("limitDeposit")} DOGE"

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
      withdraw()
    }

    play.setOnClickListener {
      loading.openDialog()
      startBot()
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
        val balanceLimit = valueFormat.dogeToDecimal(user.getString("limitDeposit").toBigDecimal())
        if (balanceValue > BigDecimal(0) && balanceValue < balanceLimit) {
          runOnUiThread {
            withdrawContent.visibility = LinearLayout.GONE
            play.isEnabled = true
            balance.text = "${valueFormat.decimalToDoge(balanceValue).toPlainString()} DOGE"
            loading.closeDialog()
          }
        } else if (balanceValue >= balanceLimit) {
          runOnUiThread {
            withdrawContent.visibility = LinearLayout.VISIBLE
            play.isEnabled = false
            balance.text = "${valueFormat.decimalToDoge(balanceValue).toPlainString()} DOGE"
            loading.closeDialog()
          }
        } else {
          runOnUiThread {
            withdrawContent.visibility = LinearLayout.GONE
            play.isEnabled = false
            balance.text = "${valueFormat.decimalToDoge(balanceValue).toPlainString()} Doge too small"
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

  private fun startBot() {
    var uniqueCode = UUID.randomUUID().toString()
    val body = HashMap<String, String>()
    body["a"] = "StartTrading"
    body["usertrade"] = user.getString("username")
    body["passwordtrade"] = user.getString("password")
    body["notrx"] = uniqueCode
    body["balanceawal"] = valueFormat.decimalToDoge(balanceValue).toPlainString()
    body["ref"] = MD5().convert(user.getString("username") + user.getString("password") + uniqueCode + "balanceawalb0d0nk111179")
    Timer().schedule(1000) {
      response = WebController(body).execute().get()
      try {
        if (response["code"] == 200) {
          if (response.getJSONObject("data")["Status"] == 0) {
            if (response.getJSONObject("data")["main"] == true) {
              val oldBalanceData = BigDecimal(response.getJSONObject("data")["saldoawalmain"].toString(), MathContext.DECIMAL32)
              uniqueCode = response.getJSONObject("data")["notrxlama"].toString()
              val profit = balanceValue - valueFormat.decimalToDoge(oldBalanceData)
              runOnUiThread {
                goTo = Intent(applicationContext, ResultActivity::class.java)
                if (profit < BigDecimal(0)) {
                  goTo.putExtra("type", 0)
                  goTo.putExtra("status", "CUT LOSS")
                  goTo.putExtra("uniqueCode", uniqueCode)
                  goTo.putExtra("balanceStart", balanceValue)
                  goTo.putExtra("balanceEnd", valueFormat.decimalToDoge(oldBalanceData))
                } else {
                  goTo.putExtra("type", 1)
                  goTo.putExtra("status", "WIN")
                  goTo.putExtra("uniqueCode", uniqueCode)
                  goTo.putExtra("balanceStart", balanceValue)
                  goTo.putExtra("balanceEnd", valueFormat.decimalToDoge(oldBalanceData))
                }
                startActivity(goTo)
                finish()
                loading.closeDialog()
              }
            } else {
              if (balanceValue < valueFormat.decimalToDoge(BigDecimal(10000))) {
                runOnUiThread {
                  Toast.makeText(applicationContext, "Your Doge Balance must more then 10000", Toast.LENGTH_LONG).show()
                  loading.closeDialog()
                }
              } else {
                if (activeChart.isChecked && activeProgressBar.isChecked) {
                  runOnUiThread {
                    goTo = Intent(applicationContext, BotActivity::class.java)
                    goTo.putExtra("uniqueCode", uniqueCode)
                    goTo.putExtra("balance", balanceValue)
                    startActivity(goTo)
                    loading.closeDialog()
                  }
                } else if (activeChart.isChecked) {
                  runOnUiThread {
                    goTo = Intent(applicationContext, BotProgressBarGoneActivity::class.java)
                    goTo.putExtra("uniqueCode", uniqueCode)
                    goTo.putExtra("balance", balanceValue)
                    startActivity(goTo)
                    loading.closeDialog()
                  }
                } else if (activeProgressBar.isChecked) {
                  runOnUiThread {
                    goTo = Intent(applicationContext, BotChartGoneActivity::class.java)
                    goTo.putExtra("uniqueCode", uniqueCode)
                    goTo.putExtra("balance", balanceValue)
                    startActivity(goTo)
                    loading.closeDialog()
                  }
                } else {
                  runOnUiThread {
                    goTo = Intent(applicationContext, BotProgressBarAndChartGoneActivity::class.java)
                    goTo.putExtra("uniqueCode", uniqueCode)
                    goTo.putExtra("balance", balanceValue)
                    startActivity(goTo)
                    loading.closeDialog()
                  }
                }
              }
            }
          } else {
            Toast.makeText(applicationContext, "One day trading is only allowed once", Toast.LENGTH_LONG).show()
            loading.closeDialog()
          }
        } else {
          Toast.makeText(
            applicationContext,
            "Your connection is not stable to do the robot process. find a place that is more likely to run the robot",
            Toast.LENGTH_LONG
          ).show()
          loading.closeDialog()
        }
      } catch (e: Exception) {
        runOnUiThread {
          Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  private fun withdraw() {
    val body = HashMap<String, String>()
    body["a"] = "Withdraw"
    body["s"] = user.getString("sessionCookie")
    body["Amount"] = "0"
    body["Address"] = user.getString("dogeWallet")
    body["Currency"] = "doge"
    Timer().schedule(1000) {
      response = DogeController(body).execute().get()
      if (response["code"] == 200) {
        runOnUiThread {
          Toast.makeText(applicationContext, "The number of satoshis queued for withdrawal.", Toast.LENGTH_SHORT).show()
          goTo = Intent(applicationContext, MainActivity::class.java)
          startActivity(goTo)
          finish()
          loading.closeDialog()
        }
      } else {
        runOnUiThread {
          Toast.makeText(applicationContext, response["data"].toString(), Toast.LENGTH_SHORT).show()
          loading.closeDialog()
          getBalance()
        }
      }
    }
  }
}
