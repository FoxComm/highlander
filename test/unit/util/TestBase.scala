package util

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpecLike, FreeSpec, MustMatchers}
import utils.Config._
import cats.data.Xor

trait TestBase extends FreeSpecLike
  with MustMatchers
  with ScalaFutures
  with TypeCheckedTripleEquals
  with CatsHelpers {

  val config = TestBase.config

  implicit class XorTestOps[G, B](val xor: B Xor G) {
    def get: G = xor.fold(l ⇒ fail(s".get on a Xor.Left: ${ l }"), r ⇒ r)
  }
}

object TestBase {
  def config = utils.Config.loadWithEnv(env = Test)
}
