package com.drobisch.synkr.sync

import com.drobisch.synkr.config.Configuration
import com.drobisch.synkr.file.FileBackend

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class LocalFSToS3Syncer(val targetBackend: FileBackend, val sourceBackend: FileBackend) extends Syncer
  with Configuration {
  override def sourceUpdate: Update = (files: ComparedFiles) => {
    val content = targetBackend.getContent(files.remoteFile.location)
    content.map { fileContent =>
      sourceBackend.putFile(files.localFile.location, fileContent, Some(files.remoteFile.version))
      fileContent.close()
    }
  }

  override def targetUpdate: Update = (files: ComparedFiles) => {
    backup(files.remoteFile, "remote")
    sourceBackend.getContent(files.localFile.location).map { fileContent =>
      targetBackend.putFile(files.remoteFile.location, fileContent, Some(files.localFile.version))
      fileContent.close()
    }
  }

  private def backup(file: VersionedFile, infix: String): Option[String] = {
    import scala.concurrent.duration._
    Await.result(targetBackend.getContent(file.location).map { fileContent =>
      val md5 = sourceBackend.putFile(
        Location(config.syncerConfiguration.backupContainer, file.location.path + s".$infix." + file.version),
        fileContent, Some(file.version)
      )
      fileContent.close()
      md5
    }, 300.seconds)
  }
}
