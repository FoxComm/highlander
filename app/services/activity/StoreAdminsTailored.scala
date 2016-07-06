package services.activity

import models.StoreAdmin
import models.traits.Originator

object StoreAdminsTailored {

  case class StoreAdminCreated(storeAdmin: StoreAdmin, admin: Originator)
      extends ActivityBase[StoreAdminCreated]

  case class StoreAdminUpdated(storeAdmin: StoreAdmin, admin: Originator)
      extends ActivityBase[StoreAdminUpdated]

  case class StoreAdminDeleted(storeAdmin: StoreAdmin, admin: Originator)
      extends ActivityBase[StoreAdminDeleted]

  case class StoreAdminStateChanged(storeAdmin: StoreAdmin,
                                    oldState: StoreAdmin.State,
                                    newState: StoreAdmin.State,
                                    admin: Originator)
      extends ActivityBase[StoreAdminStateChanged]
}
