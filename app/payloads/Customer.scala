package payloads

final case class CreateCustomer(email: String, password: String, firstName: String, lastName: String) {}

final case class UpdateCustomer(email: Option[String], password: Option[String], firstName: Option[String],
  lastName: Option[String], phoneNumber: Option[String])

final case class ToggleCustomerDisabled(disabled: Boolean)

