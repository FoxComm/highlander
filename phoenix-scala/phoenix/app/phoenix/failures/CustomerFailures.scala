package phoenix.failures

import core.failures.Failure

object CustomerFailures {

  case object CustomerMustHaveCredentials extends Failure {
    override def description: String =
      "Customer must have email set for this operation"
  }

  case object CustomerEmailNotUnique extends Failure {
    override def description = "The email address you entered is already in use"
  }

  case class CustomerHasNoDefaultAddress(accountId: Int) extends Failure {
    override def description = s"No default address found for customer with id =$accountId"
  }

  case object CustomerHasDefaultShippingAddress extends Failure {
    override def description = "customer already has default shipping address"
  }

  case class CustomerIsBlacklisted(accountId: Int) extends Failure {
    override def description = s"Customer with id = $accountId is blacklisted"
  }

  case class CustomerHasNoEmail(accountId: Int) extends Failure {
    override def description = s"Customer $accountId has no email"
  }

  case class ResetPasswordCodeInvalid(code: String) extends Failure {
    override def description = s"Reset password code $code is not valid"
  }
}
