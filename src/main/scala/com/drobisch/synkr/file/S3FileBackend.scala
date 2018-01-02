package com.drobisch.synkr.file

import java.io.InputStream

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{ObjectMetadata, S3ObjectInputStream}
import com.drobisch.synkr.config.AWSCredentialConfig
import com.drobisch.synkr.sync.{Location, VersionedFile}
import com.drobisch.synkr.util.Logging.LogTry

import scala.concurrent.Future
import scala.util.Try

object S3FileBackend {
  val scheme = "s3"
}

class S3FileBackend(credentials: AWSCredentialConfig) extends FileBackend {
  def createClient(region: Option[String]): AmazonS3 = AmazonS3ClientBuilder.standard()
    .withRegion(region.map(Regions.fromName).getOrElse(Regions.DEFAULT_REGION))
    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(credentials.awsAccessId, credentials.awsSecretKey)))
    .build()

  def getFile(location: Location): Option[VersionedFile] =
    location.parent.map { bucket: String =>
      val modifiedTime = objectMetaData(location.path, bucket, location.region)
        .map(_.getLastModified.getTime)
        .getOrElse(0L)
      VersionedFile(Location(Some(bucket), location.path, S3FileBackend.scheme), modifiedTime)
    }

  def getFileContent(location: Location): Option[S3ObjectInputStream] = LogTry {
    val s3 = createClient(location.region)
    location.parent.flatMap(bucket => Some(s3.getObject(bucket, location.path).getObjectContent))
  }.toOption.flatten

  def objectMetaData(path: String, bucket: String, region: Option[String]): Option[ObjectMetadata] = LogTry {
    val s3 = createClient(region)
    Some(s3.getObjectMetadata(bucket, path))
  }.toOption.flatten

  def putFile(location: Location, inputStream: InputStream, lastModified: Option[Long]): Option[String] = Try {
    val s3 = createClient(location.region)
    val metadata: ObjectMetadata = new ObjectMetadata()
    location
      .parent
      .flatMap(bucket => Some(s3.putObject(bucket, location.path, inputStream, metadata).getContentMd5))
  }.toOption.flatten

  override def getContent(location: Location): Future[InputStream] = getFileContent(location) match {
    case Some(inputStream) => Future.successful(inputStream)
    case None => Future.failed(new IllegalArgumentException(s"unable to get content for $location"))
  }

  override def deleteFile(location: Location): Try[Boolean] = ???
}
