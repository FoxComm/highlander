package responses

import java.time.Instant
import models.{Address, Customer, OrderShippingAddress, Region}

object Addresses {
  final case class Root(id: Int, customer: Option[Customer] = None, region: Region, name: String, address1: String,
    address2: Option[String] = None, city: String, zip: String, isDefault: Option[Boolean] = None,
    phoneNumber: Option[String] = None, deletedAt: Option[Instant])

  def build(address: Address, region: Region, isDefault: Option[Boolean] = None): Root =
    Root(id = address.id, region = region, name = address.name, address1 = address.address1, address2 = address.address2,
      city = address.city, zip = address.zip, isDefault = isDefault, phoneNumber = address.phoneNumber, deletedAt = address.deletedAt)

  def build(records: Seq[(models.Address, Region)]): Seq[Root] =
    records.map { case (address, region) ⇒ build(address, region) }

  def buildShipping(records: Seq[(models.Address, OrderShippingAddress, Region)]): Seq[Root] = {
    records.map { case (address, shippingAddress, region) ⇒
      build(address, region)
    }
  }

  def buildOneShipping(address: OrderShippingAddress, region: Region, isDefault: Boolean = false): Root = {
    Root(id = address.id, region = region, name = address.name, address1 = address.address1, address2 = address.address2,
      city = address.city, zip = address.zip, isDefault = Some(isDefault), phoneNumber = address.phoneNumber, deletedAt = None)
  }
}

