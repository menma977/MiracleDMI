package com.miracledmi.config

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class MD5 {
  fun convert(input : String) : String {
    val md5 = "MD5"
    try {
      val digest = MessageDigest
        .getInstance(md5)
      digest.update(input.toByteArray())
      val messageDigest = digest.digest()

      val hexString = StringBuilder()
      for (aMessageDigest in messageDigest) {
        var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
        while (h.length < 2) h = "0$h"
        hexString.append(h)
      }
      return hexString.toString()
    } catch (e: NoSuchAlgorithmException) {
      //e.printStackTrace()
    }
    return ""
  }
}