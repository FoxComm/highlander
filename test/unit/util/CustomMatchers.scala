package util

import cats.implicits._
import cats.data._
import org.scalatest._
import matchers._
import services._

object CustomMatchers {
  // Same as `include`, used for Failure inner Strings
  class IncludeFailureMatcher(expectedSubstring: String) extends Matcher[NonEmptyList[Failure]] {
    def apply(left: NonEmptyList[Failure]) = {
      MatchResult(
        left.exists(_.description.exists(_.contains(expectedSubstring))),
        s"""NonEmptyList of Failures does not contain "$expectedSubstring"""",
        s"""NonEmptyList of Failure contains "$expectedSubstring""""
      )
    }
  }

  def includeFailure(expectedSubstring: String) = new IncludeFailureMatcher(expectedSubstring)
}