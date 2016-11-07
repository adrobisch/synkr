package com.drobisch.synkr

import java.io.InputStream

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, S3ObjectInputStream}

import scala.util.Try

class S3FileBackend(credentialsProvider: AWSCredentialsProvider) extends FileBackend {
  val client = Helper.LogTry[AmazonS3Client](new AmazonS3Client(credentialsProvider)).toOption

  override def getFile(container: Option[String], path: String): Option[VersionedFile] =
    container.map { bucket: String =>
      VersionedFile(Some(bucket), path, objectMetaData(path, bucket).map(_.getLastModified.getTime).getOrElse(0), _ => getFileContent(path, bucket).get)
    }

  def getFileContent(path: String, bucket: String): Option[S3ObjectInputStream] = Helper.LogTry {
    client.map(_.getObject(bucket, path).getObjectContent)
  }.toOption.flatten

  def objectMetaData(path: String, bucket: String): Option[ObjectMetadata] = Helper.LogTry {
    client.map(_.getObjectMetadata(bucket, path))
  }.toOption.flatten

  override def putFile(container: Option[String], path: String, inputStream: InputStream, lastModified: Option[Long]): Option[String] = Try {
    val metadata: ObjectMetadata = new ObjectMetadata()
    container.flatMap(bucket => client.map(_.putObject(bucket, path, inputStream, metadata)).map(_.getContentMd5))
  }.toOption.flatten
}
