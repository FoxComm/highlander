package failures

object UserFailures {

  case object UserMustHaveCredentials extends Failure {
    override def description: String =
      "User must have credentials (email, name) set for this operation"
  }

  case object UserEmailNotUnique extends Failure {
    override def description = "The email address you entered is already in use"
  }

  case class UserWithAccountNotFound(accountId: Int) extends Failure {
    override def description = s"User with account id $accountId not found"
  }

  case class UserIsBlacklisted(accountId: Int) extends Failure {
    override def description = s"User with id = $accountId is blacklisted"
  }

  case class UserHasNoEmail(accountId: Int) extends Failure {
    override def description = s"User $accountId has no email"
  }

  case class ResetPasswordCodeInvalid(code: String) extends Failure {
    override def description = s"Reset password code $code is not valid"
  }
}
