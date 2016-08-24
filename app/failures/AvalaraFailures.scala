package failures

object AvalaraFailures {

  case object UnableToMatchResponse extends Failure {
    override def description: String = "Could not read response from Avalara"
  }

  case class AddressValidationFailure(message: String) extends Failure {
    override def description: String = s"Address validation failed with message: '$message'"
  }

}
