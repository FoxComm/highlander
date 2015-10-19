package services

import scala.concurrent.ExecutionContext

import models.{Order, OrderAssignment, OrderAssignments, Orders, StoreAdmin, StoreAdmins}
import responses.{AllOrders, BulkAssignmentResponse}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._

object BulkOrderUpdater {

  def assign(payload: payloads.BulkAssignment)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage): Result[BulkAssignmentResponse] = {

    val query = for {
      orders ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      admin ← StoreAdmins.findById(payload.assigneeId).result
      newAssignments = for (o ← orders; a ← admin) yield OrderAssignment(orderId = o.id, assigneeId = a.id)
      allOrders ← (OrderAssignments ++= newAssignments) >> OrderQueries.findAll
      adminNotFound = adminNotFoundFailure(admin.headOption, payload.assigneeId)
      ordersNotFound = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
    } yield BulkAssignmentResponse(allOrders, adminNotFound, ordersNotFound)

    Result.fromFuture(query.transactionally.run())
  }

  def unassign(payload: payloads.BulkAssignment)
    (implicit ec: ExecutionContext, db: Database, sortAndPage: SortAndPage): Result[BulkAssignmentResponse] = {

    val query = for {
      orders ← Orders.filter(_.referenceNumber.inSetBind(payload.referenceNumbers)).result
      adminId ← StoreAdmins.findById(payload.assigneeId).extract.map(_.id).result
      delete = OrderAssignments
        .filter(_.assigneeId === payload.assigneeId)
        .filter(_.orderId.inSetBind(orders.map(_.id)))
        .delete
      allOrders ← delete >> OrderQueries.findAll
      adminNotFound = adminNotFoundFailure(adminId.headOption, payload.assigneeId)
      ordersNotFound = ordersNotFoundFailures(payload.referenceNumbers, orders.map(_.referenceNumber))
    } yield BulkAssignmentResponse(allOrders, adminNotFound, ordersNotFound)

    Result.fromFuture(query.transactionally.run())
  }

  private def adminNotFoundFailure(a: Option[_], id: Int) = a match {
    case Some(_) ⇒ None
    case None ⇒ Some(NotFoundFailure404(StoreAdmin, id))
  }

  private def ordersNotFoundFailures(requestedRefs: Seq[String], availableRefs: Seq[String]) =
    requestedRefs.diff(availableRefs).map(NotFoundFailure404(Order, _))
}
