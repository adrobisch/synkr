package com.drobisch.synkr.util

import java.io.{File, FileInputStream}

object Hashing {

  def getMD5Hex(newFile: File): String = {
    val fileStream = new FileInputStream(newFile)
    val md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fileStream)
    fileStream.close()
    md5
  }

}
