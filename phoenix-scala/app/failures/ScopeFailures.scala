package failures

object ScopeFailures {

  object EmptyScope extends Failure {
    override def description = "Specified scope is empty!"
  }

  case class InvalidSubscope(scope: String, subscope: String) extends Failure {
    override def description =
      s"""Subscope "$subscope" is not a valid subscope of scope "$scope""""
  }
}
