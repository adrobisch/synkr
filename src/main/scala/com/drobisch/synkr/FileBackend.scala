package com.drobisch.synkr

import java.io.InputStream

import scala.concurrent.Future

trait FileBackend {
  def putFile(location: Location, inputStream: InputStream, lastModified: Option[Long]): Option[String]

  def getFile(location: Location): Option[VersionedFile]

  def getContent(location: Location): Future[InputStream]
}
