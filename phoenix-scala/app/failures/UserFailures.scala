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

  case class AccessMethodNotFound(name: String) extends Failure {
    override def description = s"Access method '$name' not found"
  }

  case class OrganizationNotFoundByName(name: String) extends Failure {
    override def description = s"Organization '$name' not found"
  }
  case class OrganizationNotFound(name: String, scope: String) extends Failure {
    override def description = s"Organization '$name' not found in scope $scope"
  }

  case class RoleNotFound(name: String, scope: String) extends Failure {
    override def description = s"Role '$name' not found in scope $scope"
  }
}
