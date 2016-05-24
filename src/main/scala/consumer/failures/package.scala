package consumer

import cats.data.NonEmptyList

package object failures {
  type Failures = NonEmptyList[Failure]

  implicit class FailureOps(val underlying: Failure) extends AnyVal {
    def single: Failures = NonEmptyList(underlying)
  }
}
