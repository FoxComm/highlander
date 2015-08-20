package util

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpec, MustMatchers}
import utils.Config._
import cats.data.Xor

trait TestBase extends FreeSpec
  with MustMatchers
  with ScalaFutures
  with TypeCheckedTripleEquals
  with CatsHelpers {

  val config = TestBase.config
}

object TestBase {
  def config = utils.Config.loadWithEnv(env = Test)
}
