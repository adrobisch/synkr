package com.drobisch.synkr

import java.io.File
import java.util.concurrent.{Executors, ScheduledFuture, TimeUnit}

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.drobisch.synkr.Helper.LogTry
import com.typesafe.config.{ConfigFactory, ConfigObject}
import org.slf4j.LoggerFactory

case class AWSCredentialConfig(awsAccessId: String, awsSecretKey: String)

trait Configuration {
  val config = AppConfiguration.load.get
}

case class AppConfiguration(syncerConfiguration: SyncerConfiguration, awsConfig: Option[AWSCredentialConfig])

object AppConfiguration {
  def load: Option[AppConfiguration] = {
    import scala.collection.JavaConverters._

    LogTry {
      val config = ConfigFactory.parseFile(appFolderFile("synkr.conf"))

      val awsCredentialConfig = for {
        key <- LogTry(config.getString("aws.access.key")).toOption
        secret <- LogTry(config.getString("aws.access.secret")).toOption
      } yield AWSCredentialConfig(key, secret)

      val syncConfigs = config.getObject("sync").entrySet().asScala.toSeq.map { entry =>
        val configObject: ConfigObject = entry.getValue.asInstanceOf[ConfigObject]

        FileSyncConfig(
          entry.getKey,
          Location(
            container = Option(configObject.get("target")).map(_.unwrapped().toString),
            path = configObject.get("targetFile").unwrapped().toString
          ),
          Location(
            container = Option(configObject.get("source")).map(_.unwrapped().toString),
            path = configObject.get("sourceFile").unwrapped().toString
          )
        )
      }

      AppConfiguration(SyncerConfiguration(syncConfigs, Some(appFolderFile("backups").getAbsolutePath)), awsCredentialConfig)
    }.toOption
  }

  def appFolderFile(fileName: String) = new File(new File(userHome, ".synkr"), fileName)

  lazy val userHome = System.getProperty("user.home")
}

trait SynkrApp extends Configuration {
  self: App =>

  val log = LoggerFactory.getLogger(getClass)

  def start: ScheduledFuture[_] = {
    val s3FileBackend = config
      .awsConfig
      .map(aws => new AWSStaticCredentialsProvider(new BasicAWSCredentials(aws.awsAccessId, aws.awsSecretKey)))
      .map(credentials => new S3FileBackend(AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).withCredentials(credentials).build()))
      .getOrElse(new S3FileBackend(AmazonS3ClientBuilder.standard().withRegion(Regions.EU_WEST_1).build()))

    val syncer = new LocalFSToS3(s3FileBackend, new LocalFileBackend)
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
