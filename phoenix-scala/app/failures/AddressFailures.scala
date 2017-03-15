package failures

object AddressFailures {
  case class NoDefaultAddressForCustomer(customerId: Int) extends Failure {
    def description: String = s"customer $customerId has no default address"
  }
}
