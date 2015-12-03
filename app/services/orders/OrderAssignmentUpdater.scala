package services.orders

import models.{Order, OrderAssignment, OrderAssignments, Orders, StoreAdmin, StoreAdmins}
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import responses.{FullOrder, ResponseWithFailuresAndMetadata, TheResponse}
import services.orders.Helpers._
import services.{Failure, Failurez, NotFoundFailure404, Result}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import scala.concurrent.ExecutionContext

object OrderAssignmentUpdater {

  def assign(refNum: String, requestedAssigneeIds: Seq[Int])(implicit db: Database, ec: ExecutionContext,
    sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): Result[TheResponse[FullOrder.Root]] = (for {

    order           ← * <~ mustFindOrderByRefNum(refNum)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result
    assignees       ← * <~ OrderAssignments.assigneesFor(order).toXor
    newAssignments  = adminIds.diff(assignees.map(_.id))
      .map(adminId ⇒ OrderAssignment(orderId = order.id, assigneeId = adminId))

    inserts   = OrderAssignments ++= newAssignments
    newOrder  ← * <~ (inserts >> Orders.findByRefNum(refNum).result.head.toXor)
    fullOrder ← * <~ FullOrder.fromOrder(newOrder).toXor
    warnings  = Failurez(requestedAssigneeIds.diff(adminIds).map(NotFoundFailure404(StoreAdmin, _)): _*)
  } yield TheResponse.build(fullOrder, warnings = warnings)).runT()

  def assign(payload: payloads.BulkAssignment)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage): Result[BulkOrderUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      orders ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      admin ← StoreAdmins.findById(payload.assigneeId).result
      newAssignments = for (o ← orders; a ← admin) yield OrderAssignment(orderId = o.id, assigneeId = a.id)
      allOrders ← (OrderAssignments ++= newAssignments) >> OrderQueries.findAll.result
      adminNotFound = adminNotFoundFailure(admin.headOption, payload.assigneeId)
      ordersNotFound = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
    } yield ResponseWithFailuresAndMetadata.fromXor(
        result = allOrders,
        addFailures = adminNotFound ++ ordersNotFound)

    query.transactionally.run()
  }

  def unassign(payload: payloads.BulkAssignment)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage): Result[BulkOrderUpdateResponse] = {

    // TODO: transfer sorting-paging metadata
    val query = for {
      orders ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      adminId ← StoreAdmins.findById(payload.assigneeId).extract.map(_.id).result
      delete = OrderAssignments
        .filter(_.assigneeId === payload.assigneeId)
        .filter(_.orderId.inSetBind(orders.map(_.id)))
        .delete
      allOrders ← delete >> OrderQueries.findAll.result
      adminNotFound = adminNotFoundFailure(adminId.headOption, payload.assigneeId)
      ordersNotFound = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
    } yield ResponseWithFailuresAndMetadata.fromXor(
        result = allOrders,
        addFailures = adminNotFound ++ ordersNotFound)

    query.transactionally.run()
  }

  private def adminNotFoundFailure(a: Option[_], id: Int) = a match {
    case Some(_) ⇒ Seq.empty[Failure]
    case None ⇒ Seq(NotFoundFailure404(StoreAdmin, id))
  }

  private def ordersNotFoundFailures(requestedRefs: Seq[String], availableRefs: Seq[String]) =
    requestedRefs.diff(availableRefs).map(NotFoundFailure404(Order, _))

}
