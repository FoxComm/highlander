package responses

import models.Customer

object CustomerResponse {
  final case class Root(
    id: Int = 0,
    email: String,
    firstName: String,
    lastName: String,
    phoneNumber: Option[String] = None,
    location: Option[String] = None,
    modality: Option[String] = None)

  def build(customer: Customer): Root =
    Root(id = customer.id, email = customer.email, firstName = customer.firstName, lastName = customer.lastName,
    phoneNumber = customer.phoneNumber, location = customer.location, modality = customer.modality)
}
