package failures

object StoreAdminFailures {

  case class AlreadyExistsWithEmail(email: String) extends Failure {
    override def description =
      s"store admin with email $email already exists"
  }
}
