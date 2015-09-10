package responses

import models.StoreAdmin

object StoreAdminResponse {
  final case class Root(
    id: Int = 0,
    email: String,
    firstName: String,
    lastName: String,
    department: Option[String])

  def build(admin: StoreAdmin): Root =
    Root(email = admin.email, firstName = admin.firstName, lastName = admin.lastName, department = admin.department)
}
