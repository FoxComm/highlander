package responses

import cats.implicits._
import models.account.User
import models.admin.StoreAdminUser

object StoreAdminResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  state: StoreAdminUser.State)
      extends ResponseItem

  def build(admin: User, storeAdminUser: StoreAdminUser): Root = {

    require(admin.accountId == storeAdminUser.accountId)
    require(admin.id == storeAdminUser.userId)

    Root(id = admin.accountId,
         email = admin.email,
         name = admin.name,
         phoneNumber = admin.phoneNumber,
         state = storeAdminUser.state)
  }
}
