package com.ambiata.notion
package io

import core._
import org.specs2._
import org.specs2.execute.AsResult
import org.specs2.matcher._
import org.specs2.specification.FixtureExample
import com.ambiata.notion.core.TemporaryType._
import com.ambiata.mundane.io._
import com.ambiata.mundane.testing.ResultTIOMatcher._
import scalaz._, Scalaz._
import TemporaryLocations._
import Arbitraries._

class LocationIOSpec extends Specification with ForeachTemporaryType with ScalaCheck { def is = section("aws") ^ s2"""

 The LocationIO class provides functions to read/write/query locations on different systems

   isDirectory             $isDirectory
   deleteAll               $deleteAll
   delete                  $delete
   read / write lines      $readWriteLines
   list                    $list
   exists                  $exists

"""

  def isDirectory = { temporaryType: TemporaryType =>
    "This location is a directory on "+temporaryType ==> {
      withLocationDir(temporaryType) { location =>
        locationIO.writeUtf8(location </> "file", "") >>
        locationIO.isDirectory(location)
      } must beOkValue(true)
    }

    "The location is a file on "+temporaryType ==> {
      withLocationFile(temporaryType) { location =>
        locationIO.writeUtf8(location, "") >>
        locationIO.isDirectory(location)
      } must beOkValue(false)
    }
  }

  def deleteAll = { temporaryType: TemporaryType =>
    "All files are deleted "+temporaryType ==> {
      withLocationDir(temporaryType) { location =>
        locationIO.writeUtf8(location </> "file1", "test") >>
        locationIO.writeUtf8(location </> "file2", "test") >>
        locationIO.deleteAll(location) >>
        locationIO.list(location)
      } must beOkValue(Nil)
    }
  }

  def delete = { temporaryType: TemporaryType =>
    "The file is deleted on "+temporaryType ==> {
      withLocationFile(temporaryType) { location =>
        locationIO.writeUtf8(location, "test") >>
        locationIO.delete(location) >>
        locationIO.exists(location)
      } must beOkValue(false)
    }
  }

  def readWriteLines = prop { (temporaryType: TemporaryType, lines: List[String]) =>
    withLocationFile(temporaryType) { location =>
      locationIO.writeUtf8Lines(location, lines) >>
      locationIO.readLines(location)
    } must beOkValue(lines)
  }


  def list = { temporaryType: TemporaryType =>
    "All files are listed on "+temporaryType ==> {
      withLocationDir(temporaryType) { location =>
        locationIO.writeUtf8(location </> "file1", "") >>
        locationIO.writeUtf8(location </> "file2", "") >>
        locationIO.list(location).map(ls => (ls.map(_.render.split("/").last), List("file1", "file2")))
      } must beOkLike { case (ls1, ls2) => ls1.toSet must_== ls2.toSet }
    }
  }

  def exists = { temporaryType: TemporaryType =>
    "There is a file on "+temporaryType ==> {
      withLocationFile(temporaryType) { location =>
        locationIO.writeUtf8(location, "") >>
        locationIO.exists(location)
      } must beOkValue(true)
    }
  }

  def locationIO = LocationIO.default

}

trait ForeachTemporaryType extends FixtureExample[TemporaryType] with MustMatchers {
  def fixture[R : AsResult](f: TemporaryType => R) =
    Seq(Posix, Hdfs, S3).toStream.map(t => AsResult(f(t))).reduceLeft(_ and _)
}