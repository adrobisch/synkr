package com.drobisch.synkr.sync

import com.drobisch.synkr.file.FileBackend
import com.drobisch.synkr.sync.Location.LocationResolver

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class LocationUpdate(locationResolver: LocationResolver,
                     backupPath: Option[String]) {
  type Update = ComparedFiles => Unit

  def sourceUpdate: Update = (files: ComparedFiles) => for {
    targetBackend <- locationResolver(files.targetFile.location)
    sourceBackend <- locationResolver(files.sourceFile.location)
    content <- targetBackend.getContent(files.targetFile.location)
  } {
    sourceBackend.putFile(files.sourceFile.location, content, Some(files.targetFile.version))
    content.close()
  }

  def targetUpdate: Update = (files: ComparedFiles) => for {
    targetBackend <- locationResolver(files.targetFile.location)
    sourceBackend <- locationResolver(files.sourceFile.location)
    content <- sourceBackend.getContent(files.sourceFile.location)
  } {
    backup(targetBackend, sourceBackend, files.targetFile, "remote")
    targetBackend.putFile(files.targetFile.location, content, Some(files.sourceFile.version))
    content.close()
  }

  private def backup(targetBackend: FileBackend, sourceBackend: FileBackend, file: VersionedFile, infix: String): Option[String] = {
    import scala.concurrent.duration._
    Await.result(targetBackend.getContent(file.location).map { fileContent =>
      val md5 = sourceBackend.putFile(
        Location(backupPath, file.location.path + s".$infix." + file.version, file.location.scheme),
        fileContent, Some(file.version)
      )
      fileContent.close()
      md5
    }, 300.seconds)
  }
}
