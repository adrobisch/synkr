package com.drobisch.synkr.util

import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Try}

trait Logging {
  val log: Logger = LoggerFactory.getLogger(getClass)
}

object Logging extends Logging {
  def LogTry[A](computation: => A): Try[A] = {
    Try(computation) recoverWith {
      case e: Throwable =>
        log.error("oops", e)
        Failure(e)
    }
  }
}
