package services.rmas

import scala.concurrent.ExecutionContext
import models.RmaLockEvents.scope._
import models._
import responses.RmaResponse
import responses.RmaLockResponse
import responses.RmaResponse.Root
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._
import utils.Slick._
import utils.Slick.implicits._

object RmaLockUpdater {

  private val updatedRma = Rmas.map(identity)

  def getLockStatus(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[RmaLockResponse.Root] = {
    val finder = Rmas.findByRefNum(refNum)

    finder.selectOneForUpdate({ rma ⇒
      val queries = for {
        event ← RmaLockEvents.findByRma(rma).mostRecentLock.one
        admin ← event.map(e ⇒ StoreAdmins.findById(e.lockedBy).extract.one).getOrElse(lift(None))
      } yield (event, admin)

      DbResult.fromDbio(queries.map { case (event, admin) ⇒
        RmaLockResponse.build(rma, event, admin)
      })
    }, checks = Set.empty)
  }

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
      } else DbResult.failure(NotLockedFailure(Rma, rma.refNum))
    }, checks = Set.empty)
  }

  private def doUnlock(rmaId: Int)(implicit ec: ExecutionContext, db: Database) = {
    Rmas.findById(rmaId).extract
      .map(_.locked)
      .updateReturning(updatedRma, false).head
      .flatMap { rma ⇒ DbResult.fromDbio(RmaResponse.fromRma(rma)) }
  }
}