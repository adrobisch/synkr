package com.drobisch.synkr

import java.io.{File, FileInputStream}
import java.util.concurrent.{Executors, TimeUnit}

import com.drobisch.synkr.config.Configuration
import com.drobisch.synkr.file.{LocalFileBackend, S3FileBackend, S3Object}
import com.drobisch.synkr.sync.LocationComparison
import com.drobisch.synkr.util.SystemTraySupport
import monix.execution.Scheduler
import org.apache.commons.codec.digest.DigestUtils
import org.backuity.clist.{Cli, _}
import org.slf4j.LoggerFactory

import scala.concurrent.{Await, duration}

trait SynkrApp extends LocationComparison with Configuration {
  val log = LoggerFactory.getLogger(getClass)

  lazy val s3FileBackend: Option[S3FileBackend] = config
    .aws
    .map(credentials => new S3FileBackend(credentials))

  lazy val localFileBackend: LocalFileBackend = new LocalFileBackend

  def startSync: Unit = {
    val interval = 5
    val unit = TimeUnit.SECONDS

    log.info(s"starting sync every $interval $unit with configuration $config")

    val scheduler = Executors.newSingleThreadScheduledExecutor()

    scheduler.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        config.sync.configs.foreach { config =>
          println(s"syncing $config")
        }
        Unit
      }
    }, 0, interval, unit)
  }
}

class Start extends Command(name = "start", description = "start the synkr sync process") with SynkrApp with Runnable {
  override def run(): Unit = {
    startSync
    SystemTraySupport.createTray(("Exit", _ => System.exit(0)))
  }
}

class Gui extends Command(name = "gui", description = "start the synkr gui") with SynkrApp with Runnable {
  override def run(): Unit = {
    SystemTraySupport.createTray(("Exit", _ => System.exit(0)))
  }
}

class Check extends Command(name = "check", description = "check md5sum of a local file against a remote file") with SynkrApp with Runnable {
  var local = arg[File](description = "local file to check")
  var remote = arg[String](description = "remote file to check")
  var region = opt[String](description = "region for remote file", default = "eu-central-1")

  override def run(): Unit = s3FileBackend.foreach { s3 =>
    println(DigestUtils.md5Hex(new FileInputStream(local)))
    val remoteLocation = remote.replaceFirst("s3://", "").split("/").toList match {
      case head :: tail => S3Object(Some(head), tail.mkString("/"), region, None)
    }

    val md5Sum = Await.result(s3.getContent(remoteLocation).map(stream => {
      val md5Sum = DigestUtils.md5Hex(stream)
      stream.close()
      md5Sum
    }).runAsync(Scheduler.global), duration.Duration.Inf)

    println(md5Sum)
  }
}

object DefaultApp extends App {
  val start = new Start

  Cli
    .parse(args)
    .withProgramName("synkr")
    .withCommands[Command with Runnable](start, new Check) match {
    case Some(command) => command.run()
    case None => start.run()
  }
}