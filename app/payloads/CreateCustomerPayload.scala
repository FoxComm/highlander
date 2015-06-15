package payloads

case class CreateCustomerPayload(email: String, password: String, firstName: String, lastName: String) {}
