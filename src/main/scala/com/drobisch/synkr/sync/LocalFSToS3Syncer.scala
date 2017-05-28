package com.drobisch.synkr.sync

import com.drobisch.synkr.config.Configuration
import com.drobisch.synkr.file.FileBackend

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

class LocalFSToS3Syncer(val remoteFileBackend: FileBackend, val localFileBackend: FileBackend) extends Syncer
  with Configuration {
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
