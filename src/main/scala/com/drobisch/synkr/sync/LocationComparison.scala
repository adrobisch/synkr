package com.drobisch.synkr.sync

import com.drobisch.synkr.config.FileSyncConfig
import com.drobisch.synkr.file.FileBackend
import com.drobisch.synkr.sync.Location.LocationResolver

case class Location(parent: Option[String],
                    path: String,
                    scheme: String,
                    region: Option[String] = None)
case class VersionedFile(location: Location, version: Long)
case class ComparedFiles(config: FileSyncConfig, sourceFile: VersionedFile, targetFile: VersionedFile)

sealed trait FileComparison
case object NoChanges extends FileComparison
case class TargetUpdate(files: ComparedFiles) extends FileComparison
case class SourceUpdate(files: ComparedFiles) extends FileComparison

object Location {
  type LocationResolver = Location => Option[FileBackend]
}

trait LocationComparison {
  def compareLocations(configs: Seq[FileSyncConfig],
                       locationResolver: LocationResolver): Seq[FileComparison] = {
    configs.flatMap(fileSync => for {
      targetBackend <- locationResolver(fileSync.target)
      sourceBackend <- locationResolver(fileSync.source)
      targetFile <- targetBackend.getFile(fileSync.target)
      sourceFile <- sourceBackend.getFile(fileSync.source)
    } yield targetFile.version.compareTo(sourceFile.version) match {
      case 0 => NoChanges
      case c if c < 0 => TargetUpdate(ComparedFiles(fileSync, sourceFile, targetFile))
      case c if c > 0 => SourceUpdate(ComparedFiles(fileSync, sourceFile, targetFile))
    })
  }
}


