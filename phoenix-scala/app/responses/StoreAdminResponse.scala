package responses

import io.circe.syntax._
import models.account.User
import models.admin.AdminData
import utils.aliases._
import utils.json.codecs._

object StoreAdminResponse {
  case class Root(id: Int = 0,
                  email: Option[String] = None,
                  name: Option[String] = None,
                  phoneNumber: Option[String] = None,
                  state: AdminData.State)
      extends ResponseItem {
    def json: Json = this.asJson
  }

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
