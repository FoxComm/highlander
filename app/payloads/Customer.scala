package payloads


final case class CreateCustomerPayload(email: String,
  name: Option[String] = None,
  password: Option[String] = None,
  isGuest: Option[Boolean] = Some(false))

final case class UpdateCustomerPayload(
  name: Option[String] = None,
  email: Option[String] = None,
  phoneNumber: Option[String] = None)

final case class ToggleCustomerDisabled(disabled: Boolean)

