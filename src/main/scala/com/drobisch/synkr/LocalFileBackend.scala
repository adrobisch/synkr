package com.drobisch.synkr

import java.io.{File, FileInputStream, InputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import Helper.LogTry

class LocalFileBackend extends FileBackend {
  override def getFile(container: Option[String], path: String): Option[VersionedFile] = LogTry {
    container
      .map(new File(_, path))
      .map(file => VersionedFile(Some(file.getParentFile.getAbsolutePath), file.getName, file.lastModified(), _ => new FileInputStream(file)))
  }.toOption.flatten

  override def putFile(container: Option[String], path: String, inputStream: InputStream, lastModified: Option[Long]): Option[String] = LogTry {
    val targetPath: Path = Paths.get(container.map(_ + File.separatorChar + path).getOrElse(path))
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
}
