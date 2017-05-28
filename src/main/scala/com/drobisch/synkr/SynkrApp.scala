package com.drobisch.synkr

import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.drobisch.synkr.config.Configuration
import com.drobisch.synkr.file.{LocalFileBackend, S3FileBackend}
import com.drobisch.synkr.sync.LocalFSToS3Syncer
import com.drobisch.synkr.util.Helper.LogTry
import com.drobisch.synkr.util.SystemTray
import org.slf4j.LoggerFactory

trait SynkrApp extends Configuration {
  self: App =>

  val log = LoggerFactory.getLogger(getClass)

  def start: ScheduledFuture[_] = {
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

object DefaultApp extends SynkrApp with App {
  start
  new SystemTray().createTray
}
