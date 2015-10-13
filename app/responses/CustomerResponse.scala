package responses

import java.time.Instant

import models.{Region, Customer}

object CustomerResponse {
  final case class Root(
    id: Int = 0,
    email: String,
    name: String,
    firstName: String,
    lastName: String,
    phoneNumber: Option[String] = None,
    location: Option[String] = None,
    modality: Option[String] = None,
    createdAt: Instant,
    disabled: Boolean,
    blacklisted: Boolean,
    rank: String,
    billingRegion: Option[String] = None,
    shippingRegion: Option[String] = None) extends ResponseItem

  def build(customer: Customer, shippingRegion: Option[Region] = None, billingRegion: Option[Region] = None): Root =
    Root(id = customer.id, email = customer.email, firstName = customer.firstName, lastName = customer.lastName,
    name = customer.firstName + " " + customer.lastName,
    phoneNumber = customer.phoneNumber, location = customer.location, modality = customer.modality,
    createdAt = customer.createdAt,
    disabled = customer.isDisabled,
    blacklisted = customer.isBlacklisted,
    rank = "top 10",
    billingRegion = billingRegion.map(_.name),
    shippingRegion = shippingRegion.map(_.name))
}
