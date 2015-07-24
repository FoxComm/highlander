package payloads

final case class CreateAddressPayload(name: String, stateId: Int, state: Option[String] = None,
  street1: String, street2: Option[String] = None, city: String, zip: String, isDefault: Boolean = false)

final case class ToggleDefaultShippingAddress(isDefault: Boolean)

