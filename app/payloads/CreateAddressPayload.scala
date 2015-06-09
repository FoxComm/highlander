package payloads

case class CreateAddressPayload(name: String, stateId: Int, street1: String, street2: Option[String],
                                city: String, zip: String)
