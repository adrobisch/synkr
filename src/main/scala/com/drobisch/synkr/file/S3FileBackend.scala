package com.drobisch.synkr.file

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, S3ObjectInputStream}
import com.drobisch.synkr.util.Helper.LogTry
import com.drobisch.synkr.sync.{Location, VersionedFile}

import scala.concurrent.Future
import scala.util.Try

class S3FileBackend(s3: AmazonS3) extends FileBackend {
  def getFile(location: Location): Option[VersionedFile] =
    location.container.map { bucket: String =>
      VersionedFile(Location(Some(bucket), location.path), objectMetaData(location.path, bucket).map(_.getLastModified.getTime).getOrElse(0))
    }

  def getFileContent(location: Location): Option[S3ObjectInputStream] = LogTry {
    location.container.flatMap(bucket => Some(s3.getObject(bucket, location.path).getObjectContent))
  }.toOption.flatten

  def objectMetaData(path: String, bucket: String): Option[ObjectMetadata] = LogTry {
    Some(s3.getObjectMetadata(bucket, path))
  }.toOption.flatten

  def putFile(location: Location, inputStream: InputStream, lastModified: Option[Long]): Option[String] = Try {
    val metadata: ObjectMetadata = new ObjectMetadata()
    location
      .container
      .flatMap(bucket => Some(s3.putObject(bucket, location.path, inputStream, metadata).getContentMd5))
  }.toOption.flatten

  override def getContent(location: Location): Future[InputStream] = getFileContent(location) match {
    case Some(inputStream) => Future.successful(inputStream)
    case None => Future.failed(new IllegalArgumentException(s"unable to get content for $location"))
  }
}
