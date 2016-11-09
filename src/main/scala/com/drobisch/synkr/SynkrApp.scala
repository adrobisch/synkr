package com.drobisch.synkr

import java.io.File
import java.util.concurrent.{Executors, TimeUnit}

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials, DefaultAWSCredentialsProviderChain}
import com.drobisch.synkr.Helper.LogTry
import com.typesafe.config.{ConfigFactory, ConfigObject}
import org.slf4j.LoggerFactory

case class AWSCredentialConfig(awsAccessId: String, awsSecretKey: String)

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
          Option(configObject.get("target")).map(_.unwrapped().toString),
          configObject.get("targetFile").unwrapped().toString,
          Option(configObject.get("source")).map(_.unwrapped().toString),
          configObject.get("sourceFile").unwrapped().toString
        )
      }

      AppConfiguration(SyncerConfiguration(syncConfigs, Some(appFolderFile("backups").getAbsolutePath)), awsCredentialConfig)
    }.toOption
  }

  def appFolderFile(fileName: String) = new File(new File(userHome, ".synkr"), fileName)

  lazy val userHome = System.getProperty("user.home")
}

trait SynkrApp {
  self: App =>

  val log = LoggerFactory.getLogger(getClass)

  def start = {
    AppConfiguration.load.map { config =>
      val s3FileBackend = config
        .awsConfig
        .map(aws => new AWSStaticCredentialsProvider(new BasicAWSCredentials(aws.awsAccessId, aws.awsSecretKey)))
        .map(new S3FileBackend(_))
        .getOrElse(new S3FileBackend(new DefaultAWSCredentialsProviderChain))

      val syncer = new Syncer(config.syncerConfiguration, s3FileBackend, new LocalFileBackend)
      val interval = 5
      val unit = TimeUnit.SECONDS

      log.info(s"starting sync every $interval $unit with configuration $config")

      val scheduler = Executors.newSingleThreadScheduledExecutor()

      scheduler.scheduleAtFixedRate(new Runnable {
        override def run(): Unit = {
          LogTry(syncer.sync)
          Unit
        }
      }, 0, interval, unit)
    }
  }

}

object DefaultApp extends SynkrApp with App {
  start.foreach { appConfig =>
    new SystemTray().createTray
  }
}
