package com.drobisch.synkr.util

import java.io.{File, FileInputStream, InputStream}

object Hashing {
  def getMD5Hex(content: InputStream): String = {
    val md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(content)
    content.close()
    md5
  }
}
