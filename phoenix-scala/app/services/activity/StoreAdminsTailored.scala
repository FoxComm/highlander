package services.activity

import models.StoreAdmin

object StoreAdminsTailored {

  case class StoreAdminCreated(storeAdmin: StoreAdmin, admin: User)
      extends ActivityBase[StoreAdminCreated]

  case class StoreAdminUpdated(storeAdmin: StoreAdmin, admin: User)
      extends ActivityBase[StoreAdminUpdated]

  case class StoreAdminDeleted(storeAdmin: StoreAdmin, admin: User)
      extends ActivityBase[StoreAdminDeleted]

  case class StoreAdminStateChanged(storeAdmin: StoreAdmin,
                                    oldState: StoreAdmin.State,
                                    newState: StoreAdmin.State,
                                    admin: User)
      extends ActivityBase[StoreAdminStateChanged]
}
