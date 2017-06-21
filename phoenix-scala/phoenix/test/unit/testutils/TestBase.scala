package testutils

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import com.typesafe.config.Config
import core.failures.Failures
import org.scalactic.{CanEqual, TypeCheckedTripleEquals}
import org.scalatest.concurrent.{AbstractPatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{FreeSpecLike, MustMatchers, OptionValues, Tag}

import phoenix.utils.FoxConfig

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

  implicit val long2int = new CanEqual[Long, Int] {
    def areEqual(a: Long, b: Int): Boolean = a == b.toLong
  }

  implicit val long2intOpt: CanEqual[Option[Long], Some[Int]] =
    new CanEqual[Option[Long], Some[Int]] {
      def areEqual(a: Option[Long], b: Some[Int]): Boolean = a == b.map(_.toLong)
    }

  implicit val long2int3tuple = new CanEqual[(Long, Long, Long), (Int, Int, Int)] {
    def areEqual(a: (Long, Long, Long), b: (Int, Int, Int)): Boolean =
      a._1 == b._1 && a._2 == b._2 && a._3 == b._3
  }

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
  System.setProperty("phoenix.env", "test")
  val bareConfig: Config = FoxConfig.unsafe
  val config: FoxConfig  = FoxConfig.config
}
