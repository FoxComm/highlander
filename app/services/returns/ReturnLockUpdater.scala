package services.returns

import models.returns._
import models.{StoreAdmin, StoreAdmins}
import responses.{ReturnLockResponse, ReturnResponse}
import services.Result
import utils.aliases._
import utils.db._

object ReturnLockUpdater {

  def getLockState(refNum: String)(implicit ec: EC, db: DB): Result[ReturnLockResponse.Root] =
    (for {
      rma   ← * <~ Returns.mustFindByRefNum(refNum)
      event ← * <~ ReturnLockEvents.latestLockByRma(rma.id).one
      admin ← * <~ event
               .map(e ⇒ StoreAdmins.findById(e.lockedBy).extract.one)
               .getOrElse(lift(None))
    } yield ReturnLockResponse.build(rma, event, admin)).runTxn()

  def lock(refNum: String, admin: StoreAdmin)(implicit ec: EC,
                                              db: DB): Result[ReturnResponse.Root] =
    (for {
      rma  ← * <~ Returns.mustFindByRefNum(refNum)
      _    ← * <~ rma.mustNotBeLocked
      _    ← * <~ Returns.update(rma, rma.copy(isLocked = true))
      _    ← * <~ ReturnLockEvents.create(ReturnLockEvent(returnId = rma.id, lockedBy = admin.id))
      rma  ← * <~ Returns.refresh(rma)
      resp ← * <~ ReturnResponse.fromRma(rma)
    } yield resp).runTxn()

  def unlock(refNum: String)(implicit ec: EC, db: DB): Result[ReturnResponse.Root] =
    (for {
      rma  ← * <~ Returns.mustFindByRefNum(refNum)
      _    ← * <~ rma.mustBeLocked
      _    ← * <~ Returns.update(rma, rma.copy(isLocked = false))
      rma  ← * <~ Returns.refresh(rma)
      resp ← * <~ ReturnResponse.fromRma(rma)
    } yield resp).runTxn()
}
