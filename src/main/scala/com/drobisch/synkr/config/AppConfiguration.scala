package com.drobisch.synkr.config

import java.io.File

import com.drobisch.synkr.sync.Location
import com.drobisch.synkr.util.Helper.LogTry
import com.typesafe.config.{ConfigFactory, ConfigObject}

import scala.collection.JavaConverters._

trait Configuration {
  val config: AppConfiguration = AppConfiguration.load.get
}

case class FileSyncConfig(id: String,
                          targetLocation: Location,
                          sourceLocation: Location,
                          removeSource: Boolean = false)

case class SyncerConfiguration(fileSyncs: Seq[FileSyncConfig], backupContainer: Option[String])

case class AWSCredentialConfig(awsAccessId: String, awsSecretKey: String)

case class AppConfiguration(syncerConfiguration: SyncerConfiguration, awsConfig: Option[AWSCredentialConfig])

object AppConfiguration {
  def load: Option[AppConfiguration] = {

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

  lazy val userHome: String = System.getProperty("user.home")
}