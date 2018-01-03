package com.drobisch.synkr.file

import java.io.{File, FileInputStream, InputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import com.drobisch.synkr.util.Logging.LogTry
import com.drobisch.synkr.sync.{Location, VersionedFile}
import com.drobisch.synkr.util.Hashing

import scala.concurrent.Future

object LocalFileBackend {
  val scheme = "file"
}

class LocalFileBackend extends FileBackend {
  override def getFile(location: Location): Option[VersionedFile] = LogTry {
    location.parent
      .map(new File(_, location.path))
      .map(file => VersionedFile(Location(Some(file.getParentFile.getAbsolutePath), file.getName, LocalFileBackend.scheme), file.lastModified()))
  }.toOption.flatten

  override def putFile(location: Location, inputStream: InputStream, lastModified: Option[Long]): Option[String] = LogTry {
    val targetPath: Path = Paths.get(location.parent.map(_ + File.separatorChar + location.path).getOrElse(location.path))
    targetPath.toFile.mkdirs()
    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

    val newFile: File = targetPath.toFile
    lastModified.map(newFile.setLastModified)

    Hashing.getMD5Hex(new FileInputStream(newFile))
  }.toOption

  override def getContent(location: Location): Future[InputStream] = {
    location.parent
      .map(new File(_, location.path))
      .map(new FileInputStream(_))
      .map(Future.successful)
      .getOrElse(Future.failed(new IllegalArgumentException(s"unable to get $location")))
  }

  override def deleteFile(location: Location): Future[Boolean] = {
    location.parent
      .map(new File(_, location.path))
      .map(file => Future.successful(file.delete()))
  }.getOrElse(Future.failed(new IllegalArgumentException(s"unable to delete $location, does not exist")))
}
