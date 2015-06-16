package payloads

case class CreateAddressPayload(name: String, stateId: Int, state: Option[String],
                                street1: String, street2: Option[String], city: String, zip: String)