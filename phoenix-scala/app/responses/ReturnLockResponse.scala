package responses

import java.time.Instant

import models.returns.{ReturnLockEvent, Return}
import models.admin.StoreAdminUser
import models.account.User

object ReturnLockResponse {
  case class Root(isLocked: Boolean, lock: Option[Lock])

  case class Lock(id: Int, lockedBy: StoreAdminResponse.Root, lockedAt: Instant)

  def build(rma: Return,
            event: Option[ReturnLockEvent],
            admin: Option[User],
            storeAdminUser: Option[StoreAdminUser]): Root = {

    (rma.isLocked, event, admin, storeAdminUser) match {
      case (true, Some(e), Some(a), Some(sa)) ⇒
        val lock =
          Lock(id = e.id, lockedBy = StoreAdminResponse.build(a, sa), lockedAt = e.lockedAt)
        Root(rma.isLocked, Some(lock))
      case _ ⇒
        Root(rma.isLocked, None)
    }
  }
}
