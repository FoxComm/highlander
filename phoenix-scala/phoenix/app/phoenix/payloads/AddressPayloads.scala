package phoenix.payloads

import phoenix.models.traits.Addressable
import shapeless._

object AddressPayloads {

  case class CreateAddressPayload(name: String,
                                  regionId: Int,
                                  address1: String,
                                  address2: Option[String] = None,
                                  city: String,
                                  zip: String,
                                  isDefault: Boolean = false,
                                  phoneNumber: Option[String] = None)
      extends Addressable[CreateAddressPayload] {
    val zipLens: Lens[CreateAddressPayload, String] = lens[CreateAddressPayload].zip
  }

  // TODO @anna: apply `Addressable` validations to each `Option` here
  case class UpdateAddressPayload(name: Option[String] = None,
                                  regionId: Option[Int] = None,
                                  state: Option[String] = None,
                                  address1: Option[String] = None,
                                  address2: Option[String] = None,
                                  city: Option[String] = None,
                                  zip: Option[String] = None,
                                  isDefault: Option[Boolean] = None,
                                  phoneNumber: Option[String] = None)
}
