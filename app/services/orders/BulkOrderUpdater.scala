package services.orders

import scala.concurrent.ExecutionContext

import cats.data.Xor
import models.{Order, OrderAssignment, OrderAssignments, Orders, StoreAdmin, StoreAdmins}
import responses.ResponseWithFailuresAndMetadata
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import services._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick._
import utils.Slick.implicits._

object BulkOrderUpdater {

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
    case None    ⇒ Seq(NotFoundFailure404(StoreAdmin, id))
  }

  private def ordersNotFoundFailures(requestedRefs: Seq[String], availableRefs: Seq[String]) =
    requestedRefs.diff(availableRefs).map(NotFoundFailure404(Order, _))
}
