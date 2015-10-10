package payloads

final case class CreateCustomer(email: String,
  password: Option[String] = None,
  name: Option[String] = None)

final case class ToggleCustomerDisabled(disabled: Boolean)

