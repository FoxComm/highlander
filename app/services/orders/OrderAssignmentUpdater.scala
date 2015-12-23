package services.orders

import models.{Order, OrderAssignment, OrderAssignments, Orders, StoreAdmin, StoreAdmins}
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import responses.{FullOrder, ResponseWithFailuresAndMetadata, TheResponse}
import services.{LogActivity, Failure, Failurez, NotFoundFailure404, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext
import models.activity.ActivityContext

object OrderAssignmentUpdater {

  def assign(admin: StoreAdmin, refNum: String, requestedAssigneeIds: Seq[Int])
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[FullOrder.Root]] = (for {

    order           ← * <~ Orders.mustFindByRefNum(refNum)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result
    assignees       ← * <~ OrderAssignments.assigneesFor(order).result.toXor
    newAssignments  = adminIds.diff(assignees.map(_.id))
      .map(adminId ⇒ OrderAssignment(orderId = order.id, assigneeId = adminId))
    _               ← * <~ OrderAssignments.createAll(newAssignments)
    newOrder        ← * <~ Orders.refresh(order).toXor
    fullOrder       ← * <~ FullOrder.fromOrder(newOrder).toXor
    warnings        = Failurez(requestedAssigneeIds.diff(adminIds).map(NotFoundFailure404(StoreAdmin, _)): _*)
    assignedAdmins  = fullOrder.assignees.filter(a ⇒ newAssignments.map(_.assigneeId).contains(a.assignee.id)).map(_.assignee)
    _               ← * <~ LogActivity.assignedToOrder(admin, fullOrder, assignedAdmins)
  } yield TheResponse.build(fullOrder, warnings = warnings)).runT()

  def assignBulk(admin: StoreAdmin, payload: payloads.BulkAssignment)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkOrderUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      orders          ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      assignee        ← StoreAdmins.findById(payload.assigneeId).result
      newAssignments  = for (o ← orders; a ← assignee) yield OrderAssignment(orderId = o.id, assigneeId = a.id)
      _               ← OrderAssignments.createAll(newAssignments)
      allOrders       ← OrderQueries.findAll.result
      adminNotFound   = adminNotFoundFailure(assignee.headOption, payload.assigneeId)
      ordersNotFound  = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
      orderRefNums    = orders.filter(o ⇒ newAssignments.map(_.orderId).contains(o.id)).map(_.referenceNumber)
      _               ← LogActivity.bulkAssignedToOrders(admin, assignee.headOption, payload.assigneeId, orderRefNums)
    } yield ResponseWithFailuresAndMetadata.fromXor(
        result = allOrders,
        addFailures = adminNotFound ++ ordersNotFound)

    query.transactionally.run()
  }

  def unassign(admin: StoreAdmin, payload: payloads.BulkAssignment)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkOrderUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      orders          ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      assignee        ← StoreAdmins.findById(payload.assigneeId).result
      _               ← OrderAssignments.filter(_.assigneeId === payload.assigneeId)
        .filter(_.orderId.inSetBind(orders.map(_.id))).delete
      allOrders       ← OrderQueries.findAll.result
      adminNotFound   = adminNotFoundFailure(assignee.map(_.id).headOption, payload.assigneeId)
      ordersNotFound  = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
      orderRefNums    = orders.filter(o ⇒ payload.referenceNumbers.contains(o.refNum)).map(_.referenceNumber)
      _               ← LogActivity.bulkUnassignedFromOrders(admin, assignee.headOption, payload.assigneeId, orderRefNums)
    } yield ResponseWithFailuresAndMetadata.fromXor(
        result = allOrders,
        addFailures = adminNotFound ++ ordersNotFound)

    query.transactionally.run()
  }

  private def adminNotFoundFailure(a: Option[_], id: Int) = a match {
    case Some(_) ⇒ Seq.empty[Failure]
    case None    ⇒ Seq(NotFoundFailure404(StoreAdmin, id))
  }

  private def ordersNotFoundFailures(requestedRefs: Seq[String], availableRefs: Seq[String]) =
    requestedRefs.diff(availableRefs).map(NotFoundFailure404(Order, _))

}
