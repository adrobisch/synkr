package com.drobisch.synkr.file

import java.io.{File, FileInputStream, InputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import com.drobisch.synkr.sync.Location
import com.drobisch.synkr.util.Hashing
import com.drobisch.synkr.util.Logging.LogTry
import monix.eval.Task

case class LocalFile(parentPath: Option[String], path: String, lastModified: Long) extends Location

class LocalFileBackend extends FileBackend[LocalFile] {
  override def getFile(location: LocalFile): Option[LocalFile] = LogTry {
    location.parentPath
      .map(new File(_, location.path))
      .map(file => LocalFile(Some(file.getParentFile.getAbsolutePath), file.getName, file.lastModified()))
  }.toOption.flatten

  override def putFile(location: LocalFile, inputStream: InputStream, lastModified: Option[Long]): Option[String] = LogTry {
    val targetPath: Path = Paths.get(location.parentPath.map(_ + File.separatorChar + location.path).getOrElse(location.path))
    targetPath.toFile.mkdirs()
    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

    val newFile: File = targetPath.toFile
    lastModified.map(newFile.setLastModified)

    Hashing.getMD5Hex(new FileInputStream(newFile))
  }.toOption

  override def getContent(location: LocalFile): Task[InputStream] = {
    location.parentPath
      .map(new File(_, location.path))
      .map(new FileInputStream(_))
      .map(Task.now)
      .getOrElse(Task.raiseError(new IllegalArgumentException(s"unable to get $location")))
  }

  override def deleteFile(location: LocalFile): Task[Boolean] = {
    location.parentPath
      .map(new File(_, location.path))
      .map(file => Task.now(file.delete()))
  }.getOrElse(Task.raiseError(new IllegalArgumentException(s"unable to delete $location, does not exist")))

  override def listFiles(location: LocalFile): Task[List[LocalFile]] = Task {
    new File(location.path).listFiles().toList.map( file =>
      LocalFile(Some(file.getParent), file.getAbsolutePath, file.lastModified())
    )
  }
}
