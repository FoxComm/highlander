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
}
