package responses

import cats.implicits._
import models.account.User
import models.admin.StoreAdminUser
import models.auth.AdminToken

object StoreAdminResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  department: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  state: StoreAdminUser.State)
      extends ResponseItem

  def build(admin: User): Root =
    Root(id = admin.accountId,
         email = admin.email.some,
         name = admin.name.some,
         department = admin.department,
         phoneNumber = admin.phoneNumber,
         state = admin.state)

  def build(admin: User): Root =
    Root(id = admin.accountId,
         email = admin.email,
         name = admin.name,
         department = admin.department,
         state = StoreAdminUser.Active)
}
