package com.drobisch.synkr.file

import java.io.InputStream

import monix.eval.Task

trait FileBackend[T] {
  def listFiles(location: T): Task[List[T]]

  def deleteFile(location: T): Task[Boolean]

  def putFile(location: T, inputStream: InputStream, lastModified: Option[Long]): Option[String]

  def getFile(location: T): Option[T]

  def getContent(location: T): Task[InputStream]
}
