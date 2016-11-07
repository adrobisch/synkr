package com.drobisch.synkr

import java.io.InputStream

import org.slf4j.LoggerFactory

case class VersionedFile(container: Option[String], name: String, version: Long, content: Unit => InputStream)

trait FileBackend {
  def getFile(container: Option[String], path: String): Option[VersionedFile]
  def putFile(container: Option[String], path: String, inputStream: InputStream, lastModified: Option[Long]): Option[String]
}

case class SyncerConfiguration(fileSyncs: Seq[FileSyncConfig], backupContainer: Option[String])

case class FileSyncConfig(id: String,
                          remoteContainer: Option[String],
                          remotePath: String,
                          localContainer: Option[String],
                          localPath: String)

class Syncer(configuration: SyncerConfiguration,
             remoteFileBackend: FileBackend,
             localFileBackend: FileBackend) {
  val log = LoggerFactory.getLogger(getClass)

  def sync: Unit = {
    configuration.fileSyncs.map(config => for {
      remoteFile <- remoteFileBackend.getFile(config.remoteContainer, config.remotePath)
      localFile <- localFileBackend.getFile(config.localContainer, config.localPath)
    } yield remoteFile.version.compareTo(localFile.version) match {
      case 0 =>
        log.debug(s"equal: $remoteFile, $localFile")

      case c if c < 0 =>
        updateRemote(localFile, remoteFile)

      case c if c > 0 =>
        updateLocal(localFile, remoteFile)
    })
  }

  def updateLocal(localFile: VersionedFile, remoteFile: VersionedFile) = {
    log.info("remote is newer, updating local:")
    backup(localFile, "local")
    val content = remoteFile.content()
    localFileBackend.putFile(localFile.container, localFile.name, content, Some(remoteFile.version))
    content.close
  }

  def updateRemote(localFile: VersionedFile, remoteFile: VersionedFile) = {
    log.info("updating remote:")
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
