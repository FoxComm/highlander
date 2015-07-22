package payloads

final case class CreateCustomer(email: String, password: String, firstName: String, lastName: String) {}

final case class ToggleCustomerDisabled(disabled: Boolean)

