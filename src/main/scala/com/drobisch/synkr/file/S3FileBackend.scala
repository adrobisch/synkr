package com.drobisch.synkr.file

import java.io.InputStream

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{ObjectMetadata, S3ObjectInputStream}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.drobisch.synkr.config.AWSCredentialConfig
import com.drobisch.synkr.sync.Location
import com.drobisch.synkr.util.Logging.LogTry
import monix.eval.Task

import scala.util.Try

case class S3Object(parentPath: Option[String], path: String, region: String, lastModified: Option[Long]) extends Location

class S3FileBackend(credentials: AWSCredentialConfig) extends FileBackend[S3Object] {
  def s3(region: String): AmazonS3 = AmazonS3ClientBuilder.standard()
    .withRegion(Regions.fromName(region))
    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(credentials.awsAccessId, credentials.awsSecretKey)))
    .build()

  def getFile(location: S3Object): Option[S3Object] =
    location.parentPath.map { bucket: String =>
      val modifiedTime = objectMetaData(location.path, bucket, location.region)
        .map(_.getLastModified.getTime)
        .getOrElse(0L)
      S3Object(Some(bucket), location.path, location.region, Some(modifiedTime))
    }

  protected def getFileContent(location: S3Object): Option[S3ObjectInputStream] = LogTry {
    location.parentPath.flatMap(bucket => Some(s3(location.region).getObject(bucket, location.path).getObjectContent))
  }.toOption.flatten

  def objectMetaData(path: String, bucket: String, region: String): Option[ObjectMetadata] = LogTry {
    Some(s3(region).getObjectMetadata(bucket, path))
  }.toOption.flatten

  def putFile(location: S3Object, inputStream: InputStream, lastModified: Option[Long]): Option[String] = Try {
    val metadata: ObjectMetadata = new ObjectMetadata()
    location
      .parentPath
      .flatMap(bucket => Some(s3(location.region).putObject(bucket, location.path, inputStream, metadata).getContentMd5))
  }.toOption.flatten

  override def getContent(location: S3Object): Task[InputStream] = getFileContent(location) match {
    case Some(inputStream) => Task.now(inputStream)
    case None => Task.raiseError(new IllegalArgumentException(s"unable to get content for $location"))
  }

  override def deleteFile(location: S3Object): Task[Boolean] = Task {
    location.parentPath.exists { bucket =>
      s3(location.region).deleteObject(bucket, location.path)
      true
    }
  }

  override def listFiles(location: S3Object): Task[List[S3Object]] = Task {
    List.empty
  }
}
