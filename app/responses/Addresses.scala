package responses

import models.{Address, Customer, State}

object Addresses {
  final case class Root(id: Int, customer: Option[Customer] = None, state: State, name: String, street1: String,
    street2: Option[String] = None, city: String, zip: String)

  def build(address: Address, state: State): Root =
    Root(id = address.id, state = state, name = address.name, street1 = address.street1, street2 = address.street2,
      city = address.city, zip = address.zip)

  def build(addresses: Seq[(models.Address, State)]): Seq[Root] =
    addresses.map { case (address, state) ⇒ build(address, state) }
}

