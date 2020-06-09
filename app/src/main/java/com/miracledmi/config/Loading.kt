package com.miracledmi.config

import android.R.style.Theme_Translucent_NoTitleBar
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.widget.TextView
import com.miracledmi.R
import java.util.*
import kotlin.concurrent.schedule

@SuppressLint("InflateParams")
class Loading(private val activity: Activity) {
  private val dialog = Dialog(activity, Theme_Translucent_NoTitleBar)
  private lateinit var loading: TextView
  private lateinit var timer: Timer

  init {
    val view = activity.layoutInflater.inflate(R.layout.activity_main, null)
    dialog.setContentView(view)
    dialog.setCancelable(false)
  }

  fun openDialog() {
    dialog.show()
    timer = Timer()
    loading = dialog.findViewById(R.id.textViewLoading)
    var countLoading = 0
    timer.schedule(500, 500) {
      activity.runOnUiThread {
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

  fun closeDialog() {
    dialog.dismiss()
    timer.cancel()
    timer.purge()
  }
}