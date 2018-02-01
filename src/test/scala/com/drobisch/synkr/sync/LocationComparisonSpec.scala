package com.drobisch.synkr.sync

import com.drobisch.synkr.config.FileSyncConfig
import com.drobisch.synkr.file.FileBackend
import org.scalamock.scalatest.proxy.MockFactory
import org.scalatest.FlatSpec

class LocationComparisonSpec extends FlatSpec with LocationComparison with MockFactory {
  behavior of "Location Comparison"

  val localFileBackendMock: FileBackend = mock[FileBackend]("local")
  val remoteFileBackendMock: FileBackend = mock[FileBackend]("remote")

  val remoteLocation = Location(Some("remote-container"), "remote-path", "remote")
  val localLocation = Location(Some("local-container"), "local-path", "local")

  val resolver: LocationResolver = {
    case Location(_, _, "remote", _) => Some(remoteFileBackendMock)
    case Location(_, _, "local", _) => Some(localFileBackendMock)
  }

  it should "update remote if local version is higher" in {
    (localFileBackendMock.getFile _).expects(localLocation).returns(Some(VersionedFile(localLocation, 1)))
    (remoteFileBackendMock.getFile _).expects(remoteLocation).returns(Some(VersionedFile(remoteLocation, 0)))

    compareLocations(Seq(FileSyncConfig(
      "test",
      remoteLocation,
      localLocation
    )), resolver)
  }

  it should "update local if remote version is higher" in {
    (localFileBackendMock.getFile _).expects(localLocation).returns(Some(VersionedFile(localLocation, 0)))
    (remoteFileBackendMock.getFile _).expects(remoteLocation).returns(Some(VersionedFile(remoteLocation, 1)))

    compareLocations(Seq(FileSyncConfig(
      "test",
      remoteLocation,
      localLocation
    )), resolver)
  }

  it should "not sync if version is the same" in {
    (localFileBackendMock.getFile _).expects(localLocation).returns(Some(VersionedFile(localLocation, 0)))
    (remoteFileBackendMock.getFile _).expects(remoteLocation).returns(Some(VersionedFile(remoteLocation, 0)))

    compareLocations(Seq(FileSyncConfig(
      "test",
      remoteLocation,
      localLocation
    )), resolver)
  }

}
