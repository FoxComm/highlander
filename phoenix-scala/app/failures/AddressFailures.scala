package failures

object AddressFailures {
  case object NoDefaultAddressForCustomer extends Failure {
    def description: String = s"No default address defined"
  }
}
