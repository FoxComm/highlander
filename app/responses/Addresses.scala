package responses

import models.{Address, Customer, OrderShippingAddress, Region}

object Addresses {
  final case class Root(id: Int, customer: Option[Customer] = None, region: Region, name: String, street1: String,
    street2: Option[String] = None, city: String, zip: String, isDefault: Option[Boolean] = None,
    phoneNumber: Option[String] = None)

  def build(address: Address, region: Region, isDefault: Option[Boolean] = None): Root =
    Root(id = address.id, region = region, name = address.name, street1 = address.street1, street2 = address.street2,
      city = address.city, zip = address.zip, isDefault = isDefault, phoneNumber = address.phoneNumber)

  def build(records: Seq[(models.Address, Region)]): Seq[Root] =
    records.map { case (address, region) ⇒ build(address, region) }

  def buildShipping(records: Seq[(models.Address, OrderShippingAddress, Region)]): Seq[Root] = {
    records.map { case (address, shippingAddress, region) ⇒
      build(address, region)
    }
  }
}

