package failures

object ApplePayFailures {
  case object CustomerTokenShouldBeValid extends Failure {
    override def description: String = "Stripe token should be valid"
  }
}
