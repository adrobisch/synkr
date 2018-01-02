package com.drobisch.synkr.file

import java.io.InputStream

import com.drobisch.synkr.sync.{Location, VersionedFile}

import scala.concurrent.Future
import scala.util.Try

trait FileBackend {
  def deleteFile(location: Location): Try[Boolean]

  def putFile(location: Location, inputStream: InputStream, lastModified: Option[Long]): Option[String]

  def getFile(location: Location): Option[VersionedFile]

  def getContent(location: Location): Future[InputStream]
}
