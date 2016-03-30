package failures

object AuthFailures {
  case object LoginFailed extends Failure {
    override def description = s"Email or password invalid"
  }

  final case class AuthFailed(reason: String) extends Failure {
    override def description = reason
  }
}