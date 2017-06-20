package phoenix.failures

import core.failures.{Failure, NotFoundFailure404}

object AddressFailures {
  object NoDefaultAddressForCustomer {
    def apply(): Failure = NotFoundFailure404("No default address defined")
  }

  case class NoCountryFound(countryCode: String) extends Failure {
    override def description: String = s"No country found for a given code: '$countryCode'"
  }

  case class NoRegionFound(regionShortName: String) extends Failure {
    override def description: String =
      s"No region found for a given short name: '$regionShortName'"
  }
}
