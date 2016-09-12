package failures

object AvalaraFailures {

  case object UnableToMatchResponse extends Failure {
    override def description: String = "Could not read response from Avalara"
  }

  case class AddressValidationFailure(message: String) extends Failure {
    override def description: String = s"Address validation failed with message: '$message'"
  }

  case class TaxApplicationFailure(message: String) extends Failure {
    override def description: String = s"Tax cannot be calculated because of: '$message'"
  }
}
