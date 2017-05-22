package phoenix.responses

import phoenix.models.account.User
import phoenix.models.admin.AdminData

object StoreAdminResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  state: AdminData.State)
      extends ResponseItem

  def build(admin: User, adminData: AdminData): Root = {

    require(admin.accountId == adminData.accountId)
    require(admin.id == adminData.userId)

    Root(id = admin.accountId,
         email = admin.email,
         name = admin.name,
         phoneNumber = admin.phoneNumber,
         state = adminData.state)
  }
}
