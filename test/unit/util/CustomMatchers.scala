package util

import cats.data._
import cats.implicits._
import org.scalatest.matchers._
import failures.{Failure, GeneralFailure}

object CustomMatchers {
  // Same as `include`, used for Failure inner Strings
  class IncludeFailureMatcher(f: Failure)
      extends Matcher[NonEmptyList[Failure]] {
    def apply(left: NonEmptyList[Failure]) = {
      MatchResult(
          left.exists(_.description.toSet.subsetOf(f.description.toSet)),
          s"""$left does not contain "$f"""",
          s"""$left contains "$f""""
      )
    }
  }

  def includeFailure(expectedSubstring: String) =
    new IncludeFailureMatcher(GeneralFailure(expectedSubstring))

  def includeFailure(f: Failure) = new IncludeFailureMatcher(f)

  def buildMatchesFailure(constraint: String, pattern: String) = {
    GeneralFailure(
        s"$constraint must fully match regular expression '$pattern'")
  }

  def includeMatchesFailure(constraint: String, pattern: String) = {
    includeFailure(buildMatchesFailure(constraint, pattern))
  }
}
