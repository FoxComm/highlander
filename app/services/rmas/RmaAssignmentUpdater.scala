package services.rmas

import models.{Rma, RmaAssignment, RmaAssignments, Rmas, StoreAdmin, StoreAdmins}
import responses.ResponseWithFailuresAndMetadata
import responses.ResponseWithFailuresAndMetadata._
import responses.RmaResponse._
import services.{Failure, NotFoundFailure404, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object RmaAssignmentUpdater {

  def assign(refNum: String, requestedAssigneeIds: Seq[Int])
    (implicit db: Database, ec: ExecutionContext): Result[FullRmaWithWarnings] = {
    val finder = Rmas.findByRefNum(refNum)

    finder.selectOne({ rma ⇒
      DbResult.fromDbio(for {
        existingAdminIds ← StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result

        alreadyAssigned ← RmaAssignments.assigneesFor(rma)
        alreadyAssignedIds = alreadyAssigned.map(_.id)

        newAssignments = existingAdminIds.diff(alreadyAssignedIds)
          .map(adminId ⇒ RmaAssignment(rmaId = rma.id, assigneeId = adminId))

        inserts = RmaAssignments ++= newAssignments
        newOrder ← inserts >> finder.result.head

        fullRma ← fromRma(rma)
        warnings = requestedAssigneeIds.diff(existingAdminIds).map(NotFoundFailure404(StoreAdmin, _))
      } yield FullRmaWithWarnings(fullRma, warnings))
    }, checks = Set.empty)
  }

  def assignBulk(payload: payloads.RmaBulkAssigneesPayload)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage): Result[BulkRmaUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      rmas ← Rmas.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      admin ← StoreAdmins.findById(payload.assigneeId).result
      newAssignments = for (r ← rmas; a ← admin) yield RmaAssignment(rmaId = r.id, assigneeId = a.id)
      allRmas ← (RmaAssignments ++= newAssignments) >> RmaQueries.findAll(Rmas).result
      adminNotFound = adminNotFoundFailure(admin.headOption, payload.assigneeId)
      rmasNotFound = rmasNotFoundFailures(payload.referenceNumbers, rmas.map(_.referenceNumber))
    } yield ResponseWithFailuresAndMetadata.fromXor(
        result = allRmas,
        addFailures = adminNotFound ++ rmasNotFound)

    query.transactionally.run()
  }

  def unassign(payload: payloads.RmaBulkAssigneesPayload)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage): Result[BulkRmaUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      rmas ← Rmas.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      adminId ← StoreAdmins.findById(payload.assigneeId).extract.map(_.id).result
      _ ← RmaAssignments.filter(_.assigneeId === payload.assigneeId).filter(_.rmaId.inSetBind(rmas.map(_.id))).delete
      allRmas ← RmaQueries.findAll(Rmas).result
      adminNotFound = adminNotFoundFailure(adminId.headOption, payload.assigneeId)
      rmasNotFound = rmasNotFoundFailures(payload.referenceNumbers, rmas.map(_.referenceNumber))
    } yield ResponseWithFailuresAndMetadata.fromXor(
        result = allRmas,
        addFailures = adminNotFound ++ rmasNotFound)

    query.transactionally.run()
  }

  private def adminNotFoundFailure(a: Option[_], id: Int) = a match {
    case Some(_) ⇒ Seq.empty[Failure]
    case None ⇒ Seq(NotFoundFailure404(StoreAdmin, id))
  }

  private def rmasNotFoundFailures(requestedRefs: Seq[String], availableRefs: Seq[String]) =
    requestedRefs.diff(availableRefs).map(NotFoundFailure404(Rma, _))
}