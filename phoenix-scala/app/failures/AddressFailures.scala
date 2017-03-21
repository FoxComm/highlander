package failures

object AddressFailures {
  object NoDefaultAddressForCustomer {
    def apply(): Failure = NotFoundFailure404("No default address defined")
  }
}
