package com.drobisch.synkr.config

import java.io.File

import com.drobisch.synkr.sync.Location
import com.drobisch.synkr.util.Logging
import com.drobisch.synkr.util.Logging.LogTry
import io.circe.parser._
import io.circe.generic.auto._

import scala.io.Source

trait Configuration {
  val config: AppConfiguration = AppConfiguration.load.get
}

case class ConfigLocation(path: String) extends Location {
  override def parentPath: Option[String] = None
}

case class FileSyncConfig(id: String,
                          target: ConfigLocation,
                          source: ConfigLocation,
                          removeSource: Option[Boolean] = Some(false))

case class SyncerConfiguration(configs: Seq[FileSyncConfig], backupContainer: Option[String])

case class AWSCredentialConfig(awsAccessId: String, awsSecretKey: String)

case class AppConfiguration(sync: SyncerConfiguration, aws: Option[AWSCredentialConfig])

object AppConfiguration extends Logging {
  def load: Option[AppConfiguration] = LogTry {
    val configJson = Source.fromFile(appFolderFile("config.json")).getLines().mkString
    val jsonOrError = decode[AppConfiguration](configJson)
    jsonOrError.left.foreach(error => log.error("error in config json", error))
    jsonOrError.right.toOption
  }.getOrElse(None)

  def appFolderFile(fileName: String) = new File(new File(userHome, ".synkr"), fileName)

  lazy val userHome: String = System.getProperty("user.home")
}