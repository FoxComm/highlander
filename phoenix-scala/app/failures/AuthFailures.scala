package failures

object AuthFailures {
  case object LoginFailed extends Failure {
    override def description = "Invalid credentials"
  }

  case class AuthFailed(reason: String) extends Failure {
    override def description = reason
  }

  case object ChangePasswordFailed extends Failure {
    override def description: String = "Old password invalid"
  }
}
