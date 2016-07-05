package responses

import models.StoreAdmin
import models.auth.AdminToken

object StoreAdminResponse {
  case class Root(id: Int = 0,
                  email: String,
                  name: String,
                  department: Option[String] = None,
                  phoneNumber: Option[String] = None)
      extends ResponseItem

  def build(admin: StoreAdmin): Root =
    Root(id = admin.id,
         email = admin.email,
         name = admin.name,
         department = admin.department,
         phoneNumber = admin.phoneNumber)

  def build(admin: AdminToken): Root =
    Root(id = admin.id,
         email = admin.email,
         name = admin.name.getOrElse(""),
         department = admin.department)
}
