package com.drobisch.synkr.file

import java.io.{File, FileInputStream, InputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import com.drobisch.synkr.util.Helper.LogTry
import com.drobisch.synkr.sync.{Location, VersionedFile}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class LocalFileBackend extends FileBackend {
  override def getFile(location: Location): Option[VersionedFile] = LogTry {
    location.container
      .map(new File(_, location.path))
      .map(file => VersionedFile(Location(Some(file.getParentFile.getAbsolutePath), file.getName), file.lastModified()))
  }.toOption.flatten

  override def putFile(location: Location, inputStream: InputStream, lastModified: Option[Long]): Option[String] = LogTry {
    val targetPath: Path = Paths.get(location.container.map(_ + File.separatorChar + location.path).getOrElse(location.path))
    targetPath.toFile.mkdirs()
    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

    val newFile: File = targetPath.toFile
    lastModified.map(newFile.setLastModified)

    getMD5Hex(newFile)
  }.toOption

  def getMD5Hex(newFile: File): String = {
    val fileStream = new FileInputStream(newFile)
    val md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fileStream)
    fileStream.close()
    md5
  }

  override def getContent(location: Location): Future[InputStream] = {
    location.container
      .map(new File(_, location.path))
      .map(new FileInputStream(_))
      .map(Future.successful)
      .getOrElse(Future.failed(new IllegalArgumentException(s"unable to get $location")))
  }

  override def deleteFile(location: Location): Try[Boolean] = {
    location.container
      .map(new File(_, location.path))
      .map(file => Try(file.delete()))
  }.getOrElse(Failure(new IllegalArgumentException(s"unable to delete $location, does not exist")))
}
