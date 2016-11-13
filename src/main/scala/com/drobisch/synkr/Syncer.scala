package com.drobisch.synkr

import java.io.InputStream

case class VersionedFile(location: Location, version: Long)

trait FileReader {
  def getFile(location: Location): Option[VersionedFile]
}

trait FileWriter {
  def putFile(location: Location, inputStream: InputStream, lastModified: Option[Long]): Option[String]
}

case class SyncerConfiguration(fileSyncs: Seq[FileSyncConfig], backupContainer: Option[String])

case class Location(container: Option[String], path: String)

case class FileSyncConfig(id: String,
                          remoteLocation: Location,
                          localLocation: Location)

case class ComparedFiles(localFile: VersionedFile, remoteFile: VersionedFile)

trait Syncer {
  type Update = ComparedFiles => Unit

  def sync(fileSyncs: Seq[FileSyncConfig], remoteFileReader: FileReader, localFileReader: FileReader)(localUpate: Update, remoteUpdate: Update)  = {
    fileSyncs.map(fileSync => for {
      remoteFile <- remoteFileReader.getFile(fileSync.remoteLocation)
      localFile <- localFileReader.getFile(fileSync.localLocation)
    } yield remoteFile.version.compareTo(localFile.version) match {
      case 0 =>

      case c if c < 0 =>
        remoteUpdate(ComparedFiles(localFile, remoteFile))

      case c if c > 0 =>
        localUpate(ComparedFiles(localFile, remoteFile))
    })
  }
}

object LocalFSToS3 {
  val updateLocal = ((comparedFiles: ComparedFiles) => {
    S3FileBackend.getFileContent(comparedFiles.remoteFile.location).map(content => )
    localFileBackend.putFile(localFile.container, localFile.name, content, Some(remoteFile.version))
    content.close
  }

  def updateRemote(localFile: VersionedFile, remoteFile: VersionedFile) = {
    backup(remoteFile, "remote")
    val content = localFile.content()
    remoteFileBackend.putFile(remoteFile.container, remoteFile.name, content, Some(localFile.version))
    content.close
  }

  def backup(file: VersionedFile, infix: String): Option[String] = {
    val content = file.content()
    val md5 = localFileBackend.putFile(configuration.backupContainer, file.name + s".$infix." + file.version, content, Some(file.version))
    content.close
    md5
  }
}
