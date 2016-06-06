package responses

import java.time.Instant

import models.returns.{ReturnLockEvent, Return}
import models.StoreAdmin

object ReturnLockResponse {
  case class Root(isLocked: Boolean, lock: Option[Lock])

  case class Lock(id: Int, lockedBy: StoreAdminResponse.Root, lockedAt: Instant)

  def build(rma: Return, event: Option[ReturnLockEvent], admin: Option[StoreAdmin]): Root = {
    (rma.isLocked, event, admin) match {
      case (true, Some(e), Some(a)) ⇒
        val lock = Lock(id = e.id, lockedBy = StoreAdminResponse.build(a), lockedAt = e.lockedAt)
        Root(rma.isLocked, Some(lock))
      case _ ⇒
        Root(rma.isLocked, None)
    }
  }
}
