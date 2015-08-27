import collection.immutable
import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor, Xor.{ left, right }
import models.Note
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import org.scalactic.{Bad, Good, Or}

package object services {
  type Failures = immutable.Seq[Failure]
  private [services] def Failures(failures: Failure*): Failures = immutable.Seq[Failure](failures: _*)

  implicit class FailuresOps(val underlying: Failure) extends AnyVal {
    def single: Failures = Failures(underlying)
  }

  type Result[A] = Future[Failures Xor A]

  object Result {
    def fromFuture[A](value: Future[A])(implicit ec: ExecutionContext): services.Result[A] = value.flatMap(good)

    def good[A](value: A):  Result[A] = Future.successful(Xor.right(value))
    def right[A](value: A): Result[A] = good(value)
    def left[A](failure: Failure): Result[A] = failures(failure)

    def failures(failures: Failure*): Result[Nothing] =
      Future.successful(Xor.left(Failures(failures: _*)))

    def failures(theFailures: Failures): Result[Nothing] =
      failures(theFailures: _*)

    def failure(failure: Failure): Result[Nothing] =
      failures(failure)
  }
}
