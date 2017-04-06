package failures

object ApplePayFailures {
  case class CustomerShouldPayExactAmount(expect: Int, provided: Int) extends Failure {
    override def description: String =
      s"Customer expect to pay exactly $expect while received $provided"
  }

  case object CustomerTokenShouldBeValid extends Failure {
    override def description: String = "Stripe token should be valid"
  }
}
