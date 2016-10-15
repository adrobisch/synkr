package com.drobisch.synkr

import java.io.InputStream

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata

import scala.util.Try

class S3FileBackend(credentialsProvider: AWSCredentialsProvider) extends FileBackend {
  val client = Helper.LogTry[AmazonS3Client](new AmazonS3Client(credentialsProvider)).toOption

  override def getFile(container: Option[String], path: String): Option[VersionedFile] = Helper.LogTry {
    container
      .flatMap(bucket => client.map(_.getObject(bucket, path)))
      .map(s3Object => VersionedFile(Some(s3Object.getBucketName), s3Object.getKey, s3Object.getObjectMetadata.getLastModified.getTime, s3Object.getObjectContent))
  }.toOption.flatten

  override def putFile(container: Option[String], path: String, inputStream: InputStream, lastModified: Option[Long]): Option[String] = Try {
    val metadata: ObjectMetadata = new ObjectMetadata()
    container.flatMap(bucket => client.map(_.putObject(bucket, path, inputStream, metadata)).map(_.getContentMd5))
  }.toOption.flatten
}
