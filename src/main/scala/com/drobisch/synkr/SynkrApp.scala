package com.drobisch.synkr

import java.io.{File, FileInputStream}
import java.util.concurrent.{Executors, TimeUnit}

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.drobisch.synkr.config.Configuration
import com.drobisch.synkr.file.{LocalFileBackend, S3FileBackend}
import com.drobisch.synkr.sync.{LocalFSToS3Syncer, Location}
import com.drobisch.synkr.util.Helper.LogTry
import com.drobisch.synkr.util.SystemTraySupport
import org.apache.commons.codec.digest.DigestUtils
import org.backuity.clist.Cli
import org.backuity.clist._
import org.slf4j.LoggerFactory

import scala.concurrent.Await

trait SynkrApp extends Configuration {
  val log = LoggerFactory.getLogger(getClass)

  def startSync: Unit = {
    val s3FileBackend = config
      .awsConfig
      .map(aws => new AWSStaticCredentialsProvider(new BasicAWSCredentials(aws.awsAccessId, aws.awsSecretKey)))
      .map(credentials => new S3FileBackend(AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).withCredentials(credentials).build()))
      .getOrElse(new S3FileBackend(AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()))

    val syncer = new LocalFSToS3Syncer(s3FileBackend, new LocalFileBackend)
    val interval = 5
    val unit = TimeUnit.SECONDS

    log.info(s"starting sync every $interval $unit with configuration $config")

    val scheduler = Executors.newSingleThreadScheduledExecutor()

    scheduler.scheduleAtFixedRate(new Runnable {
      override def run(): Unit = {
        LogTry(syncer.sync(config.syncerConfiguration.fileSyncs))
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

class Check extends Command(name = "check", description = "check md5sum of a local file against a remote file") with Runnable with Configuration {
  var local = arg[File](description = "local file to check")
  var remote = arg[String](description = "remote file to check")

  override def run(): Unit = {
    val s3FileBackend: S3FileBackend = config
      .awsConfig
      .map(aws => new AWSStaticCredentialsProvider(new BasicAWSCredentials(aws.awsAccessId, aws.awsSecretKey)))
      .map(credentials => new S3FileBackend(AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).withCredentials(credentials).build()))
      .getOrElse(new S3FileBackend(AmazonS3ClientBuilder.standard().withRegion(Regions.EU_CENTRAL_1).build()))

    println(DigestUtils.md5Hex(new FileInputStream(local)))
    val remoteLocation = remote.replaceFirst("s3://", "").split("/").toList match {
      case head :: tail => Location(Some(head), tail.mkString("/"))
    }
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._

    val md5Sum = Await.result(s3FileBackend.getContent(remoteLocation).map(stream => {
      val md5Sum = DigestUtils.md5Hex(stream)
      stream.close()
      md5Sum
    }), Duration.Inf)
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