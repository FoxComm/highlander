package responses

import java.time.Instant

import models.{Region, Customer}

object CustomerResponse {
  final case class Root(
    id: Int = 0,
    email: String,
    firstName: String,
    lastName: String,
    phoneNumber: Option[String] = None,
    location: Option[String] = None,
    modality: Option[String] = None,
    createdAt: Instant,
    isDisabled: Boolean,
    rank: String,
    billRegion: Option[String],
    shipRegion: Option[String])

  def build(customer: Customer, shipRegion: Option[Region] = None, billRegion: Option[Region] = None): Root =
    Root(id = customer.id, email = customer.email, firstName = customer.firstName, lastName = customer.lastName,
    phoneNumber = customer.phoneNumber, location = customer.location, modality = customer.modality,
    createdAt = customer.createdAt,
    isDisabled = customer.disabled,
    rank = "top 10",
    billRegion = billRegion.map(_.name),
    shipRegion = shipRegion.map(_.name))
}
