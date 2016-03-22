package failures

object CustomerFailures {

  case object CustomerEmailNotUnique extends Failure {
    override def description = "The email address you entered is already in use"
  }

  final case class CustomerHasNoDefaultAddress(customerId: Int) extends Failure {
    override def description = s"No default address found for customer with id =$customerId"
  }

  case object CustomerHasDefaultShippingAddress extends Failure {
    override def description = "customer already has default shipping address"
  }

}
