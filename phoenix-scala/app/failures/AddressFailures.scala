package failures

object AddressFailures {
  object NoDefaultAddressForCustomer {
    def apply(): Failure = NotFoundFailure404("No default address defined")
  }

  case class NoCountryFound(countryCode: String) extends Failure {
    override def description: String = s"No country found for a given code: '$countryCode'"
  }
}
