package failures

object ScopeFailures {
  case object ImproperScope extends Failure {
    override def description = "The scope specified is invalid"
  }
}
