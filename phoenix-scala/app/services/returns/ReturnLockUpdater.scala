package services.returns

import models.returns._
import models.account._
import responses.{ReturnLockResponse, ReturnResponse}
import utils.aliases._
import utils.db._

object ReturnLockUpdater {

  def getLockState(refNum: String)(implicit ec: EC, db: DB): DbResultT[ReturnLockResponse.Root] =
    for {
      rma   ← * <~ Returns.mustFindByRefNum(refNum)
      event ← * <~ ReturnLockEvents.latestLockByRma(rma.id).one
      admin ← * <~ event
               .map(e ⇒ Users.findByAccountId(e.lockedBy).extract.one)
               .getOrElse(lift(None))
    } yield ReturnLockResponse.build(rma, event, admin)

  def lock(refNum: String, admin: User)(implicit ec: EC, db: DB): DbResultT[ReturnResponse.Root] =
    for {
      rma ← * <~ Returns.mustFindByRefNum(refNum)
      _   ← * <~ rma.mustNotBeLocked
      _   ← * <~ Returns.update(rma, rma.copy(isLocked = true))
      _ ← * <~ ReturnLockEvents.create(
             ReturnLockEvent(returnId = rma.id, lockedBy = admin.accountId))
      rma  ← * <~ Returns.refresh(rma)
      resp ← * <~ ReturnResponse.fromRma(rma)
    } yield resp

  def unlock(refNum: String)(implicit ec: EC, db: DB): DbResultT[ReturnResponse.Root] =
    for {
      rma  ← * <~ Returns.mustFindByRefNum(refNum)
      _    ← * <~ rma.mustBeLocked
      _    ← * <~ Returns.update(rma, rma.copy(isLocked = false))
      rma  ← * <~ Returns.refresh(rma)
      resp ← * <~ ReturnResponse.fromRma(rma)
    } yield resp
}
