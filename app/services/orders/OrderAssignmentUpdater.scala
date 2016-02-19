package services.orders

import models.order._
import models.{NotificationSubscription, StoreAdmin, StoreAdmins}
import payloads.BulkAssignment
import responses.TheResponse
import responses.order.FullOrder
import services.Util._
import services.{NotificationManager, LogActivity, OrderAssigneeNotFound, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext
import models.activity.{Dimension, ActivityContext}

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
    notFoundAdmins  = diffToFailures(requestedAssigneeIds, adminIds, StoreAdmin)
    assignedAdmins  = fullOrder.assignees.filter(a ⇒ newAssignments.map(_.assigneeId).contains(a.assignee.id)).map(_.assignee)
    _               ← * <~ LogActivity.assignedToOrder(admin, fullOrder, assignedAdmins)
    _               ← * <~ NotificationManager.subscribe(adminIds = assignedAdmins.map(_.id), dimension = Dimension.order,
      reason = NotificationSubscription.Assigned, objectIds = Seq(order.referenceNumber))
  } yield TheResponse.build(fullOrder, errors = notFoundAdmins)).runTxn()

  def unassign(admin: StoreAdmin, refNum: String, assigneeId: Int)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[FullOrder.Root] = (for {

    order           ← * <~ Orders.mustFindByRefNum(refNum)
    assignee        ← * <~ StoreAdmins.mustFindById404(assigneeId)
    assignment      ← * <~ OrderAssignments.byAssignee(assignee).one.mustFindOr(OrderAssigneeNotFound(refNum, assigneeId))
    _               ← * <~ OrderAssignments.byAssignee(assignee).delete
    fullOrder       ← * <~ FullOrder.fromOrder(order).toXor
    _               ← * <~ LogActivity.unassignedFromOrder(admin, fullOrder, assignee)
    _               ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assigneeId), dimension = Dimension.order,
      reason = NotificationSubscription.Assigned, objectIds = Seq(order.referenceNumber))
  } yield fullOrder).runTxn()

  def assignBulk(admin: StoreAdmin, payload: BulkAssignment)(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkOrderUpdateResponse] = (for {
    // TODO: transfer sorting-paging metadata
    orders          ← * <~ Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result.toXor
    assignee        ← * <~ StoreAdmins.mustFindById400(payload.assigneeId)
    newAssignments  = for (o ← orders) yield OrderAssignment(orderId = o.id, assigneeId = assignee.id)
    _               ← * <~ OrderAssignments.createAll(newAssignments)
    response        ← * <~ OrderQueries.findAll
    ordersNotFound  = diffToFlatFailures(payload.referenceNumbers, orders.map(_.referenceNumber), Order)
    orderRefNums    = orders.filter(o ⇒ newAssignments.map(_.orderId).contains(o.id)).map(_.referenceNumber)
    _               ← * <~ LogActivity.bulkAssignedToOrders(admin, assignee, orderRefNums)
    _               ← * <~ NotificationManager.subscribe(adminIds = Seq(assignee.id), dimension = Dimension.order,
      reason = NotificationSubscription.Watching, objectIds = orders.map(_.referenceNumber)).value
  } yield response.copy(errors = ordersNotFound)).runTxn()

  def unassignBulk(admin: StoreAdmin, payload: BulkAssignment)(implicit ec: ExecutionContext, db: Database,
    sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkOrderUpdateResponse] = (for {
    // TODO: transfer sorting-paging metadata
    orders    ← * <~ Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
    assignee  ← * <~ StoreAdmins.mustFindById400(payload.assigneeId)
    _         ← * <~ OrderAssignments.filter(_.assigneeId === payload.assigneeId)
                                     .filter(_.orderId.inSetBind(orders.map(_.id))).delete
    response  ← * <~ OrderQueries.findAll
    ordersNotFound = diffToFlatFailures(payload.referenceNumbers, orders.map(_.referenceNumber), Order)
    refNums   = orders.filter(o ⇒ payload.referenceNumbers.contains(o.refNum)).map(_.referenceNumber)
    _         ← * <~ LogActivity.bulkUnassignedFromOrders(admin, assignee, refNums)
    _         ← * <~ NotificationManager.unsubscribe(adminIds = Seq(assignee.id), dimension = Dimension.order,
      reason = NotificationSubscription.Watching, objectIds = orders.map(_.referenceNumber)).value
  } yield response.copy(errors = ordersNotFound)).runTxn()
}
