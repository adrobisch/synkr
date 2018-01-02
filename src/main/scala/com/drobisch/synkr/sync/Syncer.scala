package com.drobisch.synkr.sync

import com.drobisch.synkr.config.FileSyncConfig
import com.drobisch.synkr.file.FileBackend

case class VersionedFile(location: Location, version: Long)

case class Location(container: Option[String], path: String)

case class ComparedFiles(localFile: VersionedFile, remoteFile: VersionedFile)

trait Syncer {
  type Update = ComparedFiles => Unit

  val targetBackend: FileBackend

  val sourceBackend: FileBackend

  def sourceUpdate: Update

  def targetUpdate: Update

  def sync(configs: Seq[FileSyncConfig])  = {
    configs.map(fileSync => for {
      targetFile <- targetBackend.getFile(fileSync.targetLocation)
      sourceFile <- sourceBackend.getFile(fileSync.sourceLocation)
    } yield targetFile.version.compareTo(sourceFile.version) match {
      case 0 =>

      case c if c < 0 =>
        targetUpdate(ComparedFiles(sourceFile, targetFile))
        if (fileSync.removeSource) {
          sourceBackend.deleteFile(fileSync.sourceLocation)
        }
      case c if c > 0 =>
        sourceUpdate(ComparedFiles(sourceFile, targetFile))
    })
  }
}


