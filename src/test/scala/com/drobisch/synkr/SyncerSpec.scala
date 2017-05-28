package com.drobisch.synkr

import org.scalamock.function.MockFunction1
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.FlatSpec

class SyncerSpec extends FlatSpec with MockFactory {
  behavior of "Syncer"

  val localFileBackendMock: FileBackend = mock[FileBackend]("local")
  val remoteFileBackendMock: FileBackend = mock[FileBackend]("remote")

  val remoteUpdateMock: MockFunction1[ComparedFiles, Unit] = mockFunction[ComparedFiles, Unit]("remote-update")
  val localUpdateMock: MockFunction1[ComparedFiles, Unit] = mockFunction[ComparedFiles, Unit]("local-update")

  val remoteLocation = Location(Some("remote-container"), "remote-path")
  val localLocation = Location(Some("local-container"), "local-path")

  def syncerWithMocks = new Syncer {
    override def remoteUpdate: Update = remoteUpdateMock
    override def localUpdate: Update = localUpdateMock

    override val localFileBackend: FileBackend = localFileBackendMock
    override val remoteFileBackend: FileBackend = remoteFileBackendMock
  }

  it should "update remote if local version is higher" in {
    (localFileBackendMock.getFile _).expects(localLocation).returns(Some(VersionedFile(localLocation, 1)))
    (remoteFileBackendMock.getFile _).expects(remoteLocation).returns(Some(VersionedFile(remoteLocation, 0)))

    remoteUpdateMock expects ComparedFiles(VersionedFile(localLocation, 1), VersionedFile(remoteLocation, 0))

    syncerWithMocks.sync(Seq(FileSyncConfig(
      "test",
      remoteLocation,
      localLocation
    )))
  }

  it should "update local if remote version is higher" in {
    (localFileBackendMock.getFile _).expects(localLocation).returns(Some(VersionedFile(localLocation, 0)))
    (remoteFileBackendMock.getFile _).expects(remoteLocation).returns(Some(VersionedFile(remoteLocation, 1)))

    localUpdateMock expects ComparedFiles(VersionedFile(localLocation, 0), VersionedFile(remoteLocation, 1))

    syncerWithMocks.sync(Seq(FileSyncConfig(
      "test",
      remoteLocation,
      localLocation
    )))
  }

  it should "not sync if version is the same" in {
    (localFileBackendMock.getFile _).expects(localLocation).returns(Some(VersionedFile(localLocation, 0)))
    (remoteFileBackendMock.getFile _).expects(remoteLocation).returns(Some(VersionedFile(remoteLocation, 0)))

    syncerWithMocks.sync(Seq(FileSyncConfig(
      "test",
      remoteLocation,
      localLocation
    )))
  }

}
