package services

import scala.concurrent.ExecutionContext
import models.RmaLockEvents.scope._
import models._
import responses.RmaResponse
import responses.RmaResponse.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._
import utils.Slick._
import utils.Slick.implicits._

object LockAwareRmaUpdater {

  private val updatedRma = Rmas.map(identity)

  def lock(refNum: String, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val finder = Rmas.findByRefNum(refNum)

    finder.selectOneForUpdate { rma ⇒
      val lock = finder.map(_.locked).updateReturning(updatedRma, true).head
      val blame = RmaLockEvents += RmaLockEvent(rmaId = rma.id, lockedBy = admin.id)

      DbResult.fromDbio(blame >> lock.flatMap(RmaResponse.fromRma))
    }
  }

  def unlock(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[Root] = {
    Rmas.findByRefNum(refNum).selectOneForUpdate ({ rma ⇒
      if (rma.locked) {
        RmaLockEvents.findByRma(rma).mostRecentLock.one.flatMap { _ ⇒ doUnlock(rma.id) }
      } else DbResult.failure(GeneralFailure("Return is not locked"))
    }, checks = Set.empty)
  }

  private def doUnlock(rmaId: Int)(implicit ec: ExecutionContext, db: Database): DbResult[Root] = {
    Rmas.findById(rmaId).extract
      .map(_.locked)
      .updateReturning(updatedRma, false).head
      .flatMap(rma ⇒ DbResult.fromDbio(RmaResponse.fromRma(rma)))
  }
}
