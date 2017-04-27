package testutils

import akka.util.Timeout
import com.typesafe.config.Config
import failures.Failures
import java.util.concurrent.TimeUnit
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.{AbstractPatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{FreeSpecLike, MustMatchers, OptionValues, Tag}
import utils.FoxConfig

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

  implicit class EitherTestOps[G, B](val either: Either[B, G]) {
    def get: G = either.fold(l ⇒ fail(s".get on a Either.left: $l"), r ⇒ r)
  }

  implicit class FailuresTestOps(val failures: Failures) {
    def getMessage: String = failures.head.description
  }
}

object TestBase {
  val bareConfig: Config = FoxConfig.unsafe
  val config: FoxConfig  = FoxConfig.config
}
