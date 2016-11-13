package com.drobisch.synkr

import java.io.InputStream

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, S3ObjectInputStream}

import scala.util.Try

object S3FileBackend {
  val client = Helper.LogTry[AmazonS3Client](new AmazonS3Client(new DefaultAWSCredentialsProviderChain)).toOption

  def getFile(location: Location): Option[VersionedFile] =
    location.container.map { bucket: String =>
      VersionedFile(Location(Some(bucket), location.path), objectMetaData(location.path, bucket).map(_.getLastModified.getTime).getOrElse(0))
    }

  def getFileContent(location: Location): Option[S3ObjectInputStream] = Helper.LogTry {
    location.container.flatMap(bucket => client.map(_.getObject(bucket, location.path))).map(_.getObjectContent)
  }.toOption.flatten

  def objectMetaData(path: String, bucket: String): Option[ObjectMetadata] = Helper.LogTry {
    client.map(_.getObjectMetadata(bucket, path))
  }.toOption.flatten

  def putFile(location: Location, inputStream: InputStream, lastModified: Option[Long]): Option[String] = Try {
    val metadata: ObjectMetadata = new ObjectMetadata()
    location.container.flatMap(bucket => client.map(_.putObject(bucket, location.path, inputStream, metadata)).map(_.getContentMd5))
  }.toOption.flatten
}
