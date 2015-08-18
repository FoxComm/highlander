package payloads

final case class CreateAddressPayload(name: String, stateId: Int, state: Option[String] = None,
  street1: String, street2: Option[String] = None, city: String, zip: String, isDefault: Boolean = false,
  phoneNumber: Option[String] = None)

final case class UpdateAddressPayload(name: Option[String] = None, stateId: Option[Int] = None,
  state: Option[String] = None, street1: Option[String] = None, street2: Option[String] = None,
  city: Option[String] = None, zip: Option[String] = None, isDefault: Option[Boolean] = None,
  phoneNumber: Option[String] = None)

final case class ToggleDefaultShippingAddress(isDefault: Boolean)

