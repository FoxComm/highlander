import models.location.Address
import payloads.AddressPayloads.CreateAddressPayload

package object util {

  implicit class AddressToPayload(val address: Address) extends AnyVal {
    def toPayload =
      CreateAddressPayload(name = address.name,
                           regionId = address.regionId,
                           zip = address.zip,
                           city = address.city,
                           address1 = address.address1,
                           address2 = address.address2,
                           phoneNumber = address.phoneNumber,
                           isDefault = address.isDefaultShipping)
  }

}
