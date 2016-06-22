package services.activity

import models.StoreAdmin
import models.traits.Originator

object StoreAdminsTailored {

  case class StoreAdminCreated(coupon: StoreAdmin, admin: Originator)
      extends ActivityBase[StoreAdminCreated]

  case class StoreAdminUpdated(coupon: StoreAdmin, admin: Originator)
      extends ActivityBase[StoreAdminUpdated]

  case class StoreAdminDeleted(coupon: StoreAdmin, admin: Originator)
      extends ActivityBase[StoreAdminDeleted]
}
