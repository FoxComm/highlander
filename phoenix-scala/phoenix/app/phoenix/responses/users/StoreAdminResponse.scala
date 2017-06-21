package phoenix.responses.users

import phoenix.models.account.User
import phoenix.models.admin.AdminData
import phoenix.responses.ResponseItem

case class StoreAdminResponse(id: Int = 0,
                              email: Option[String] = None,
                              name: Option[String] = None,
                              phoneNumber: Option[String] = None,
                              state: AdminData.State)
    extends ResponseItem

object StoreAdminResponse {

  def build(admin: User, adminData: AdminData): StoreAdminResponse = {

    require(admin.accountId == adminData.accountId)
    require(admin.id == adminData.userId)

    StoreAdminResponse(id = admin.accountId,
                       email = admin.email,
                       name = admin.name,
                       phoneNumber = admin.phoneNumber,
                       state = adminData.state)
  }
}
