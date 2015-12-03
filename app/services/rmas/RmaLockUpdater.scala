package services.rmas

import models.{RmaLockEvent, RmaLockEvents, Rmas, StoreAdmin, StoreAdmins}
import responses.{RmaLockResponse, RmaResponse}
import services.Result
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object RmaLockUpdater {

  def getLockStatus(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[RmaLockResponse.Root] = (for {
    rma   ← * <~ Rmas.mustFindByRefNum(refNum)
    event ← * <~ RmaLockEvents.latestLockByRma(rma.id).one.toXor
    admin ← * <~ event.map(e ⇒ StoreAdmins.findById(e.lockedBy).extract.one).getOrElse(lift(None)).toXor
  } yield RmaLockResponse.build(rma, event, admin)).runT()

  def lock(refNum: String, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[RmaResponse.Root] = (for {
    rma  ← * <~ Rmas.mustFindByRefNum(refNum)
    _    ← * <~ rma.mustNotBeLocked
    _    ← * <~ Rmas.update(rma, rma.copy(isLocked = true))
    _    ← * <~ RmaLockEvents.create(RmaLockEvent(rmaId = rma.id, lockedBy = admin.id))
    resp ← * <~ Rmas.refresh(rma).flatMap(RmaResponse.fromRma).toXor
  } yield resp).runT()

  def unlock(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[RmaResponse.Root] = (for {
    rma  ← * <~ Rmas.mustFindByRefNum(refNum)
    _    ← * <~ rma.mustBeLocked
    _    ← * <~ Rmas.update(rma, rma.copy(isLocked = false))
    resp ← * <~ Rmas.refresh(rma).flatMap(RmaResponse.fromRma).toXor
  } yield resp).runT()
}
