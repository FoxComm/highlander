package responses

import java.time.Instant

import models.{Rma, RmaLockEvent, StoreAdmin}

object RmaLockResponse {
  final case class Root(
    isLocked: Boolean,
    lock: Option[Lock])

  final case class Lock(
    id: Int,
    lockedBy: StoreAdminResponse.Root,
    lockedAt: Instant)

  def build(rma: Rma, event: Option[RmaLockEvent], admin: Option[StoreAdmin]): Root = {
    (rma.locked, event, admin) match {
      case (true, Some(e), Some(a)) ⇒
        val lock = Lock(id = e.id, lockedBy = StoreAdminResponse.build(a), lockedAt = e.lockedAt)
        Root(rma.locked, Some(lock))
      case _ ⇒
        Root(rma.locked, None)
    }
  }
}