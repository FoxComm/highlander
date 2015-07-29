package responses

import models.{OrderShippingAddress, Address, Customer, State}

object Addresses {
  final case class Root(id: Int, customer: Option[Customer] = None, state: State, name: String, street1: String,
    street2: Option[String] = None, city: String, zip: String, isDefault: Option[Boolean] = None)

  def build(address: Address, state: State, isDefault: Option[Boolean] = None): Root =
    Root(id = address.id, state = state, name = address.name, street1 = address.street1, street2 = address.street2,
      city = address.city, zip = address.zip, isDefault = isDefault)

  def build(records: Seq[(models.Address, State)]): Seq[Root] =
    records.map { case (address, state) ⇒ build(address, state) }

  def buildShipping(records: Seq[(models.Address, OrderShippingAddress, State)]): Seq[Root] = {
    records.map { case (address, shippingAddress, state) ⇒
      build(address, state)
    }
  }
}

