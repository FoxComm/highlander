package responses

import models.StoreAdmin

object StoreAdminResponse {
  final case class Root(
    id: Int = 0,
    email: String,
    name: String,
    department: Option[String]) extends ResponseItem

  def build(admin: StoreAdmin): Root =
    Root(
      id = admin.id,
      email = admin.email,
      name = admin.name,
      department = admin.department)
}
