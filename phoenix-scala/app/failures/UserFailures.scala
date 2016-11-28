package failures

import models.account._

object UserFailures {

  case object UserMustHaveCredentials extends Failure {
    override def description: String =
      "User must have email credentials set for this operation"
  }

  case object UserEmailNotUnique extends Failure {
    override def description = "The email address you entered is already in use"
  }

  object UserWithAccountNotFound {
    def apply(accountId: Int) = NotFoundFailure404(User, accountId)
  }

  case class UserIsBlacklisted(accountId: Int) extends Failure {
    override def description = s"User with id = $accountId is blacklisted"
  }

  case class UserIsMigrated(accountId: Int) extends Failure {
    override def description = s"User with id = $accountId is migrated and has to reset password"
  }

  case class UserHasNoEmail(accountId: Int) extends Failure {
    override def description = s"User $accountId has no email"
  }

  case class ResetPasswordCodeInvalid(code: String) extends Failure {
    override def description = s"Reset password code $code is not valid"
  }

  object AccessMethodNotFound {
    def apply(name: String) = NotFoundFailure404(AccountAccessMethod, name)
  }

  object OrganizationNotFoundByName {
    def apply(name: String) = NotFoundFailure404(Organization, name)
  }

  object OrganizationNotFound {
    def apply(name: String, scope: String) = NotFoundFailure404(Organization, s"$name+$scope")
  }

  object OrganizationNotFoundWithDomain {
    def apply(domain: String) = NotFoundFailure404(Organization, s"$domain")
  }

  object RoleNotFound {
    def apply(name: String, scope: String) = NotFoundFailure404(Role, s"$name+$scope")
  }

  case class AlreadyExistsWithEmail(email: String) extends Failure {
    override def description =
      s"user with email $email already exists"
  }
}
