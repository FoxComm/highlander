package responses

import cats.implicits._
import models.StoreAdmin
import models.auth.AdminToken

object StoreAdminResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  department: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  state: StoreAdmin.State)
      extends ResponseItem

  def build(admin: StoreAdmin): Root =
    Root(id = admin.id,
         email = admin.email.some,
         name = admin.name.some,
         department = admin.department,
         phoneNumber = admin.phoneNumber,
         state = admin.state)

  def build(admin: AdminToken): Root =
    Root(id = admin.id,
         email = admin.email,
         name = admin.name,
         department = admin.department,
         state = StoreAdmin.Active)
}
