package services.rmas

import models.rma.{RmaLockEvents, RmaLockEvent, Rmas}
import models.{StoreAdmin, StoreAdmins}
import responses.{RmaLockResponse, RmaResponse}
import services.Result
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object RmaLockUpdater {

  def getLockState(refNum: String)(implicit ec: EC, db: DB): Result[RmaLockResponse.Root] = (for {
    rma   ← * <~ Rmas.mustFindByRefNum(refNum)
    event ← * <~ RmaLockEvents.latestLockByRma(rma.id).one.toXor
    admin ← * <~ event.map(e ⇒ StoreAdmins.findById(e.lockedBy).extract.one).getOrElse(lift(None)).toXor
  } yield RmaLockResponse.build(rma, event, admin)).runTxn()

  def lock(refNum: String, admin: StoreAdmin)(implicit ec: EC, db: DB): Result[RmaResponse.Root] = (for {
    rma  ← * <~ Rmas.mustFindByRefNum(refNum)
    _    ← * <~ rma.mustNotBeLocked
    _    ← * <~ Rmas.update(rma, rma.copy(isLocked = true))
    _    ← * <~ RmaLockEvents.create(RmaLockEvent(rmaId = rma.id, lockedBy = admin.id))
    resp ← * <~ Rmas.refresh(rma).flatMap(RmaResponse.fromRma).toXor
  } yield resp).runTxn()

  def unlock(refNum: String)(implicit ec: EC, db: DB): Result[RmaResponse.Root] = (for {
    rma  ← * <~ Rmas.mustFindByRefNum(refNum)
    _    ← * <~ rma.mustBeLocked
    _    ← * <~ Rmas.update(rma, rma.copy(isLocked = false))
    resp ← * <~ Rmas.refresh(rma).flatMap(RmaResponse.fromRma).toXor
  } yield resp).runTxn()
}
