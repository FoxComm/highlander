package services.activity

import models.account.User
import models.admin.StoreAdminUser

object StoreAdminsTailored {

  case class StoreAdminCreated(storeAdmin: User, admin: User)
      extends ActivityBase[StoreAdminCreated]

  case class StoreAdminUpdated(storeAdmin: User, admin: User)
      extends ActivityBase[StoreAdminUpdated]

  case class StoreAdminDeleted(storeAdmin: User, admin: User)
      extends ActivityBase[StoreAdminDeleted]

  case class StoreAdminStateChanged(storeAdmin: User,
                                    oldState: StoreAdminUser.State,
                                    newState: StoreAdminUser.State,
                                    admin: User)
      extends ActivityBase[StoreAdminStateChanged]
}
