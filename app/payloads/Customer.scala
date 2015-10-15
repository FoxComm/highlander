package payloads


final case class CreateCustomerPayload(email: String,
  name: Option[String] = None,
  password: Option[String] = None,
  isGuest: Option[Boolean] = Some(false))

final case class ToggleCustomerDisabled(disabled: Boolean)

