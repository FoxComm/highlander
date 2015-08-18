import collection.immutable
import concurrent.Future

import org.scalactic.{Bad, Good, Or}

package object services {

  type Failures = immutable.Seq[Failure]
  private [services] def Failures(failures: Failure*): Failures = immutable.Seq[Failure](failures: _*)

  implicit class FailuresOps(val underlying: Failure) extends AnyVal {
    def single: Failures = Failures(underlying)
  }

  type Result[A] = Future[A Or Failures]

  object Result {
    def good[A](order: A): Result[A] =
      Future.successful(Good(order).asInstanceOf[A Or Failures])

    def failures(failures: Failure*): Result[Nothing] =
      Future.successful(Bad(Failures(failures: _*)))

    def failures(theFailures: Failures): Result[Nothing] =
      failures(theFailures: _*)

    def failure(failure: Failure): Result[Nothing] =
      failures(failure)
  }
}
