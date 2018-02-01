package com.drobisch.synkr.sync

trait Location {
  def path: String
  def parentPath: Option[String]
}

trait Versioned[T] {
  def version(t: T): Long
}

sealed trait FileComparison
case object NoChanges extends FileComparison
case object RightNewer extends FileComparison
case object LeftNewer extends FileComparison

trait LocationComparison {
  def compareLocations(a: Location, b: Location)(implicit versioned: Versioned[Location]): FileComparison = {
    versioned.version(a).compareTo(versioned.version(b)) match {
      case 0 => NoChanges
      case c if c < 0 => RightNewer
      case c if c > 0 => LeftNewer
    }
  }
}


