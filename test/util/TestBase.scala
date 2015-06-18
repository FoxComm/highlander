package util

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}

trait TestBase extends FreeSpec
  with MustMatchers
  with ScalaFutures
