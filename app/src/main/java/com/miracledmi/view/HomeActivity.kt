package com.miracledmi.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
  private lateinit var clipboardManager: ClipboardManager
  private lateinit var clipData: ClipData

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

  private var limitDepositDefault = BigDecimal(0.000000000, MathContext.DECIMAL32).setScale(8, BigDecimal.ROUND_HALF_DOWN)
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

    when {
      user.getString("limitDeposit").isEmpty() -> {
        maxDeposit.text = "$limitDepositDefault DOGE"
      }
      else -> {
        maxDeposit.text = "${user.getString("limitDeposit")} DOGE"
      }
    }

    activeChart.isChecked = config.getBoolean("chart")
    activeProgressBar.isChecked = config.getBoolean("progressBar")

    activeChart.setOnCheckedChangeListener { _, isChecked ->
      config.setBoolean("chart", isChecked)
    }

    activeProgressBar.setOnCheckedChangeListener { _, isChecked ->
      config.setBoolean("progressBar", isChecked)
    }

    copy.setOnClickListener {
      clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipData = ClipData.newPlainText("Wallet", wallet.text.toString())
      clipboardManager.primaryClip = clipData
      Toast.makeText(applicationContext, "Dompet Doge telah disalin", Toast.LENGTH_LONG).show()
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
        val balanceLimit = when {
          user.getString("limitDeposit").isEmpty() -> {
            valueFormat.dogeToDecimal(limitDepositDefault)
          }
          else -> {
            valueFormat.dogeToDecimal(user.getString("limitDeposit").toBigDecimal())
          }
        }
        if (valueFormat.decimalToDoge(balanceValue) >= BigDecimal(10000) && balanceValue <= balanceLimit) {
          runOnUiThread {
            if (user.getString("fakeBalance") == "0" || user.getString("fakeBalance").isEmpty()) {
              play.text = "Re Withdraw"
              withdrawContent.visibility = LinearLayout.GONE
              play.isEnabled = true
              balance.text = "${valueFormat.decimalToDoge(balanceValue).toPlainString()} DOGE"
              loading.closeDialog()
            } else {
              play.text = getString(R.string.play)
              withdrawContent.visibility = LinearLayout.GONE
              play.isEnabled = true
              balance.text = "${valueFormat.decimalToDoge(user.getString("fakeBalance").toBigDecimal()).toPlainString()} DOGE"
              loading.closeDialog()
            }
          }
        } else if (balanceValue > balanceLimit) {
          runOnUiThread {
            play.text = getString(R.string.play)
            withdrawContent.visibility = LinearLayout.VISIBLE
            play.isEnabled = false
            balance.text = "${valueFormat.decimalToDoge(balanceValue).toPlainString()} DOGE terlalu tinggi"
            loading.closeDialog()
          }
        } else {
          runOnUiThread {
            play.text = getString(R.string.play)
            if (balanceValue <= BigDecimal(0)) {
              withdrawContent.visibility = LinearLayout.GONE
            } else {
              withdrawContent.visibility = LinearLayout.VISIBLE
            }
            play.isEnabled = false
            balance.text = "${valueFormat.decimalToDoge(balanceValue).toPlainString()} DOGE terlalu kecil"
            loading.closeDialog()
          }
        }
      } else {
        runOnUiThread {
          play.text = getString(R.string.play)
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
          if (response.getJSONObject("data")["Status"] == "0") {
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
                  goTo.putExtra("startBalance", balanceValue)
                } else {
                  goTo.putExtra("type", 1)
                  goTo.putExtra("status", "WIN")
                  goTo.putExtra("uniqueCode", uniqueCode)
                  goTo.putExtra("startBalance", balanceValue)
                }
                startActivity(goTo)
                finish()
                loading.closeDialog()
              }
            } else {
              if (balanceValue < valueFormat.decimalToDoge(BigDecimal(10000))) {
                runOnUiThread {
                  Toast.makeText(applicationContext, "Saldo Doge Anda harus lebih dari 10000 DOGE", Toast.LENGTH_LONG).show()
                  loading.closeDialog()
                }
              } else {
                if (activeChart.isChecked && activeProgressBar.isChecked) {
                  runOnUiThread {
                    goTo = Intent(applicationContext, BotActivity::class.java)
                    goTo.putExtra("uniqueCode", uniqueCode)
                    goTo.putExtra("balance", balanceValue)
                    startActivity(goTo)
                    finish()
                    loading.closeDialog()
                  }
                } else if (activeChart.isChecked) {
                  runOnUiThread {
                    goTo = Intent(applicationContext, BotProgressBarGoneActivity::class.java)
                    goTo.putExtra("uniqueCode", uniqueCode)
                    goTo.putExtra("balance", balanceValue)
                    startActivity(goTo)
                    finish()
                    loading.closeDialog()
                  }
                } else if (activeProgressBar.isChecked) {
                  runOnUiThread {
                    goTo = Intent(applicationContext, BotChartGoneActivity::class.java)
                    goTo.putExtra("uniqueCode", uniqueCode)
                    goTo.putExtra("balance", balanceValue)
                    startActivity(goTo)
                    finish()
                    loading.closeDialog()
                  }
                } else {
                  runOnUiThread {
                    goTo = Intent(applicationContext, BotProgressBarAndChartGoneActivity::class.java)
                    goTo.putExtra("uniqueCode", uniqueCode)
                    goTo.putExtra("balance", balanceValue)
                    startActivity(goTo)
                    finish()
                    loading.closeDialog()
                  }
                }
              }
            }
          } else {
            runOnUiThread {
              Toast.makeText(applicationContext, "Perdagangan satu hari hanya diperbolehkan satu kali", Toast.LENGTH_LONG).show()
              loading.closeDialog()
            }
          }
        } else {
          runOnUiThread {
            Toast.makeText(
              applicationContext,
              response["data"].toString(),
              Toast.LENGTH_LONG
            ).show()
            loading.closeDialog()
          }
        }
      } catch (e: Exception) {
        runOnUiThread {
          loading.closeDialog()
          Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
        }
      }
    }
  }

  private fun withdraw() {
    val body = HashMap<String, String>()
    body["a"] = "Withdraw"
    body["s"] = user.getString("key")
    body["Amount"] = "0"
    body["Address"] = user.getString("walletWithdraw")
    body["Currency"] = "doge"
    Timer().schedule(1000) {
      println(body)
      response = DogeController(body).execute().get()
      when {
        response["code"] == 200 -> {
          runOnUiThread {
            Toast.makeText(applicationContext, "Jumlah satoshi yang antri untuk ditarik.", Toast.LENGTH_SHORT).show()
            goTo = Intent(applicationContext, MainActivity::class.java)
            startActivity(goTo)
            finish()
            loading.closeDialog()
          }
        }
        else -> {
          runOnUiThread {
            Toast.makeText(applicationContext, response["data"].toString(), Toast.LENGTH_SHORT).show()
            loading.closeDialog()
            getBalance()
          }
        }
      }
    }
  }
}
