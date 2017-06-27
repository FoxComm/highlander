package phoenix.services.activity

import phoenix.models.account.User
import phoenix.models.admin.AdminData

object StoreAdminsTailored {

  case class StoreAdminCreated(storeAdmin: User, admin: Option[User], code: Option[String])
      extends ActivityBase[StoreAdminCreated]

  case class StoreAdminUpdated(storeAdmin: User, admin: User) extends ActivityBase[StoreAdminUpdated]

  case class StoreAdminDeleted(storeAdmin: User, admin: User) extends ActivityBase[StoreAdminDeleted]

  case class StoreAdminStateChanged(storeAdmin: User,
                                    oldState: AdminData.State,
                                    newState: AdminData.State,
                                    admin: User)
      extends ActivityBase[StoreAdminStateChanged]
}
