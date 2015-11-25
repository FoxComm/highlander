package util

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Tag, OptionValues, FreeSpecLike, FreeSpec, MustMatchers}
import services.Failures
import utils.Config._
import cats.data.Xor

trait TestBase extends FreeSpecLike
  with MustMatchers
  with ScalaFutures
  with OptionValues
  with TypeCheckedTripleEquals
  with CatsHelpers {

  val config = TestBase.config

  object Tags {
    object Slow extends Tag("tags.Slow")
    object External extends Tag("tags.External")
  }

  implicit class XorTestOps[G, B](val xor: B Xor G) {
    def get: G = xor.fold(l ⇒ fail(s".get on a Xor.Left: ${ l }"), r ⇒ r)
  }

  implicit class FailuresTestOps(val failures: Failures) {
    def getMessage: String = failures.head.description.headOption.value
  }
}

object TestBase {
  def config = utils.Config.loadWithEnv(env = Test)
}
