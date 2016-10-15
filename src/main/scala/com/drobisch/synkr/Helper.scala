package com.drobisch.synkr

import org.slf4j.LoggerFactory

import scala.util.{Failure, Try}

/**
  * Created by adrobisch on 10/13/16.
  */
object Helper {
  val log = LoggerFactory.getLogger(getClass)

  def LogTry[A](computation: => A): Try[A] = {
    Try(computation) recoverWith {
      case e: Throwable =>
        log.error("oops", e)
        Failure(e)
    }
  }
}
