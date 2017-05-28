package com.drobisch.synkr.sync

import com.drobisch.synkr.config.FileSyncConfig
import com.drobisch.synkr.file.FileBackend

case class VersionedFile(location: Location, version: Long)

case class Location(container: Option[String], path: String)

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


