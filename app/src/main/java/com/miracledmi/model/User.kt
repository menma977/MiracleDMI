package com.miracledmi.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

@SuppressLint("CommitPrefEdits")
class User(context: Context) {
  private val sharedPreferences: SharedPreferences
  private val sharedPreferencesEditor: SharedPreferences.Editor

  companion object {
    private const val userData = "user"
  }

  init {
    sharedPreferences = context.getSharedPreferences(userData, Context.MODE_PRIVATE)
    sharedPreferencesEditor = sharedPreferences.edit()
  }

  fun setInteger(id: String, value: Int) {
    sharedPreferencesEditor.putInt(id, value)
    sharedPreferencesEditor.commit()
  }

  fun setString(id: String, value: String) {
    sharedPreferencesEditor.putString(id, value)
    sharedPreferencesEditor.commit()
  }

  fun setBoolean(id: String, value: Boolean) {
    sharedPreferencesEditor.putBoolean(id, value)
    sharedPreferencesEditor.commit()
  }

  fun setLong(id: String, value: Long) {
    sharedPreferencesEditor.putLong(id, value)
    sharedPreferencesEditor.commit()
  }

  fun getInteger(id: String): Int {
    return sharedPreferences.getInt(id, 0)
  }

  fun getString(id: String): String {
    return sharedPreferences.getString(id, "")!!
  }

  fun getBoolean(id: String): Boolean {
    return sharedPreferences.getBoolean(id, false)
  }

  fun getLong(id: String): Long {
    return sharedPreferences.getLong(id, 0)
  }

  fun clear() {
    sharedPreferences.edit().clear().apply()
    sharedPreferencesEditor.clear()
  }
}