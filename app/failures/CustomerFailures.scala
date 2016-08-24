package failures

object CustomerFailures {

  case object CustomerMustHaveCredentials extends Failure {
    override def description: String =
      "Customer must have credentials (email, name) set for this operation"
  }

  case object CustomerEmailNotUnique extends Failure {
    override def description = "The email address you entered is already in use"
  }

  case class CustomerHasNoDefaultAddress(customerId: Int) extends Failure {
    override def description = s"No default address found for customer with id =$customerId"
  }

  case object CustomerHasDefaultShippingAddress extends Failure {
    override def description = "customer already has default shipping address"
  }

  case class CustomerIsBlacklisted(customerId: Int) extends Failure {
    override def description = s"Customer with id = $customerId is blacklisted"
  }

  case class PasswordResetAlreadyInitiated(email: String) extends Failure {
    override def description = s"Password reset procedure already initated for email $email"
  }

  case object CustomerHasNoEmail extends Failure {
    override def description = "Customer don't have email"
  }

  case object ResetPasswordCodeInvalid extends Failure {
    override def description = "Reset password code is not valid"
  }
}
