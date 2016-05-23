package payloads

object AddressPayloads {

  case class CreateAddressPayload(name: String, regionId: Int, address1: String, address2: Option[String] = None,
    city: String, zip: String, isDefault: Boolean = false, phoneNumber: Option[String] = None)

  case class UpdateAddressPayload(name: Option[String] = None, regionId: Option[Int] = None,
    state: Option[String] = None, address1: Option[String] = None, address2: Option[String] = None,
    city: Option[String] = None, zip: Option[String] = None, isDefault: Option[Boolean] = None,
    phoneNumber: Option[String] = None)

}
