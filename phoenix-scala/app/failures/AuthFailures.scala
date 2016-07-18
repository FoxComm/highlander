package failures

object AuthFailures {
  case object LoginFailed extends Failure {
    override def description = "Email or password invalid"
  }

  case class AuthFailed(reason: String) extends Failure {
    override def description = reason
  }
}
