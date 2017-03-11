package testutils

import java.util.concurrent.TimeUnit
import akka.util.Timeout
import cats.data.Xor
import failures.Failures
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.{AbstractPatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{FreeSpecLike, MustMatchers, OptionValues, Tag}
import utils.{Environment, FoxConfig}

trait TestBase
    extends FreeSpecLike
    with MustMatchers
    with AbstractPatienceConfiguration
    with ScalaFutures
    with OptionValues
    with TypeCheckedTripleEquals
    with CatsHelpers {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(
      timeout = Span(10, Seconds),
      interval = Span(15, Milliseconds)
  )

  implicit val timeout: Timeout = Timeout(10, TimeUnit.SECONDS)

  object Tags {
    object Slow     extends Tag("tags.Slow")
    object External extends Tag("tags.External")
  }

  implicit class XorTestOps[G, B](val xor: B Xor G) {
    def get: G = xor.fold(l ⇒ fail(s".get on a Xor.Left: $l"), r ⇒ r)
  }

  implicit class FailuresTestOps(val failures: Failures) {
    def getMessage: String = failures.head.description
  }
}

object TestBase {
  val bareConfig = FoxConfig.unsafe
  val config     = FoxConfig.config
}
