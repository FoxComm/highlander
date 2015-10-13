package payloads


final case class CreateCustomer(email: String,
  name: Option[String] = None,
  password: Option[String] = None)

final case class ToggleCustomerDisabled(disabled: Boolean)

