package util

import cats.data._
import org.scalatest._
import matchers._
import services.Failure

object CustomMatchers {
  // Same as `include`, used for Failure inner Strings
  class IncludeFailureMatcher(expectedSubstring: String) extends Matcher[NonEmptyList[Failure]] {
    def apply(left: NonEmptyList[Failure]) = {
      val stringValue = left.head.description.head
      MatchResult(
        stringValue.indexOf(expectedSubstring) >= 0,
        s"""String $stringValue does not contain "$expectedSubstring"""",
        s"""String $stringValue contains "$expectedSubstring""""
      )
    }
  }

  def includeFailure(expectedSubstring: String) = new IncludeFailureMatcher(expectedSubstring)
}