package services.orders

import scala.concurrent.ExecutionContext

import models.{Order, OrderAssignment, OrderAssignments, Orders, StoreAdmin, StoreAdmins}
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import responses.{FullOrder, FullOrderWithWarnings, ResponseWithFailuresAndMetadata}
import services._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.Slick.DbResult
import utils.Slick.implicits._

object OrderAssignmentUpdater {

  def assign(refNum: String, requestedAssigneeIds: Seq[Int])(implicit db: Database, ec: ExecutionContext,
    sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): Result[FullOrderWithWarnings] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOne({ order ⇒
      DbResult.fromDbio(for {
        existingAdminIds ← StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result

        alreadyAssigned ← OrderAssignments.assigneesFor(order)
        alreadyAssignedIds = alreadyAssigned.map(_.id)

        newAssignments = existingAdminIds.diff(alreadyAssignedIds)
          .map(adminId ⇒ OrderAssignment(orderId = order.id, assigneeId = adminId))

        inserts = OrderAssignments ++= newAssignments
        newOrder ← inserts >> finder.result.head

        fullOrder ← FullOrder.fromOrder(newOrder)
        warnings = requestedAssigneeIds.diff(existingAdminIds).map(NotFoundFailure404(StoreAdmin, _))
      } yield FullOrderWithWarnings(fullOrder, warnings))
    }, checks = Set.empty)
  }

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
