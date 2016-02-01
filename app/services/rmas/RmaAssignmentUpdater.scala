package services.rmas

import scala.concurrent.ExecutionContext

import models.{Rma, RmaAssignment, RmaAssignments, Rmas, StoreAdmin, StoreAdmins}
import responses.{RmaResponse, TheResponse}
import services.Util._
import services.{NotFoundFailure400, Result, RmaAssigneeNotFound}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

object RmaAssignmentUpdater {

  def assign(refNum: String, requestedAssigneeIds: Seq[Int])
    (implicit db: Database, ec: ExecutionContext): Result[TheResponse[RmaResponse.Root]] = (for {
    rma          ← * <~ Rmas.mustFindByRefNum(refNum)
    realAdminIds ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result
    assigned     ← * <~ RmaAssignments.assigneesFor(rma).toXor
    _            ← * <~ RmaAssignments.createAll(realAdminIds.diff(assigned.map(_.id)).map { adminId ⇒
                          RmaAssignment(rmaId = rma.id, assigneeId = adminId)
                        })
    newRma       ← * <~ Rmas.refresh(rma).toXor
    fullRma      ← * <~ RmaResponse.fromRma(newRma).toXor
    notFoundAdmins = diffToFailures(requestedAssigneeIds, realAdminIds, StoreAdmin)
  } yield TheResponse.build(fullRma, errors = notFoundAdmins)).runTxn()

  def unassign(admin: StoreAdmin, refNum: String, assigneeId: Int)
    (implicit db: Database, ec: ExecutionContext): Result[TheResponse[RmaResponse.Root]] = (for {
    rma             ← * <~ Rmas.mustFindByRefNum(refNum)
    assignee        ← * <~ StoreAdmins.mustFindById(assigneeId)
    assignment      ← * <~ RmaAssignments.byAssignee(assignee).one.mustFindOr(RmaAssigneeNotFound(refNum, assigneeId))
    _               ← * <~ RmaAssignments.byAssignee(assignee).delete
    fullRma         ← * <~ RmaResponse.fromRma(rma).toXor
  } yield TheResponse.build(fullRma)).runTxn()

  def assignBulk(payload: payloads.RmaBulkAssigneesPayload)(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage): Result[BulkRmaUpdateResponse] = (for {
    rmas     ← * <~ Rmas.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result.toXor
    admin    ← * <~ StoreAdmins.mustFindById(payload.assigneeId, id ⇒ NotFoundFailure400(StoreAdmin, id))
    _        ← * <~ RmaAssignments.createAll(for (r ← rmas) yield RmaAssignment(rmaId = r.id, assigneeId = admin.id))
    response ← * <~ RmaQueries.findAllDbio(Rmas)
    rmasNotFound = diffToFlatFailures(payload.referenceNumbers, rmas.map(_.referenceNumber), Rma)
  } yield response.copy(errors = rmasNotFound)).runTxn()

  def unassignBulk(payload: payloads.RmaBulkAssigneesPayload)(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage): Result[BulkRmaUpdateResponse] = (for {
    rmas ← * <~ Rmas.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result.toXor
    _    ← * <~ StoreAdmins.mustFindById(payload.assigneeId, id ⇒ NotFoundFailure400(StoreAdmin, id))
    _    ← * <~ RmaAssignments.filter(_.assigneeId === payload.assigneeId)
                              .filter(_.rmaId.inSetBind(rmas.map(_.id)))
                              .delete
    resp ← * <~ RmaQueries.findAllDbio(Rmas)
    rmasNotFound = diffToFlatFailures(payload.referenceNumbers, rmas.map(_.referenceNumber), Rma)
  } yield resp.copy(errors = rmasNotFound)).runTxn()
}
