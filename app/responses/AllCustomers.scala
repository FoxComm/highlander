package responses

import java.time.Instant

import models.{Customer, Region}

object AllCustomers {
  final case class Root(
    id: Int,
    email: String,
    name: String,
    joinedAt: Instant,
    blacklisted: Boolean,
    rank: String,
    billRegion: Option[String],
    shipRegion: Option[String])

  def build(customer: Customer, shipRegion: Option[Region] = None, billRegion: Option[Region] = None): Root = {
    Root(id = customer.id,
      email =  customer.email,
      name = customer.firstName + " " + customer.lastName,
      joinedAt = customer.createdAt,
      blacklisted = customer.disabled,
      rank = "top 10",
      billRegion = billRegion.map(_.name),
      shipRegion = shipRegion.map(_.name))
  }
}
