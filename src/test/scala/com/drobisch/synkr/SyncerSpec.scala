package com.drobisch.synkr

import java.io.InputStream

import org.scalatest.FlatSpec

class SyncerSpec extends FlatSpec {
  behavior of "Syncer"

  it should "sync" in {
    val aFileSync: FileSyncConfig = FileSyncConfig("testSync", Some("remoteContainer"), "remotePath", Some("localContainer"), "localPath")
    val configuration: SyncerConfiguration = SyncerConfiguration(Seq(aFileSync), None)
    new Syncer(configuration, testBackend, testBackend).sync
  }

  val testBackend = new FileBackend {
    override def putFile(container: Option[String], path: String, inputStream: InputStream, lastModified: Option[Long]): Option[String] = ???

    override def getFile(container: Option[String], path: String): Option[VersionedFile] = ???
  }
}
