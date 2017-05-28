package com.drobisch.synkr

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

case class VersionedFile(location: Location, version: Long)

case class SyncerConfiguration(fileSyncs: Seq[FileSyncConfig], backupContainer: Option[String])

case class Location(container: Option[String], path: String)

case class FileSyncConfig(id: String,
                          remoteLocation: Location,
                          localLocation: Location)

case class ComparedFiles(localFile: VersionedFile, remoteFile: VersionedFile)

trait Syncer {
  type Update = ComparedFiles => Unit

  val remoteFileBackend: FileBackend

  val localFileBackend: FileBackend

  def localUpdate: Update

  def remoteUpdate: Update

  def sync(configs: Seq[FileSyncConfig])  = {
    configs.map(fileSync => for {
      remoteFile <- remoteFileBackend.getFile(fileSync.remoteLocation)
      localFile <- localFileBackend.getFile(fileSync.localLocation)
    } yield remoteFile.version.compareTo(localFile.version) match {
      case 0 =>

      case c if c < 0 =>
        remoteUpdate(ComparedFiles(localFile, remoteFile))

      case c if c > 0 =>
        localUpdate(ComparedFiles(localFile, remoteFile))
    })
  }
}

class LocalFSToS3(val remoteFileBackend: FileBackend, val localFileBackend: FileBackend) extends Syncer with Configuration {
  override def localUpdate = (files: ComparedFiles) => {
    val content = remoteFileBackend.getContent(files.remoteFile.location)
    content.map { fileContent =>
      localFileBackend.putFile(files.localFile.location, fileContent, Some(files.remoteFile.version))
      fileContent.close()
    }
  }

  override def remoteUpdate = (files: ComparedFiles) => {
    backup(files.remoteFile, "remote")
    localFileBackend.getContent(files.localFile.location).map { fileContent =>
      remoteFileBackend.putFile(files.remoteFile.location, fileContent, Some(files.localFile.version))
      fileContent.close()
    }
  }

  private def backup(file: VersionedFile, infix: String): Option[String] = {
    import scala.concurrent.duration._
    Await.result(remoteFileBackend.getContent(file.location).map { fileContent =>
      val md5 = localFileBackend.putFile(
        Location(config.syncerConfiguration.backupContainer, file.location.path + s".$infix." + file.version),
        fileContent, Some(file.version)
      )
      fileContent.close()
      md5
    }, 300.seconds)
  }
}
