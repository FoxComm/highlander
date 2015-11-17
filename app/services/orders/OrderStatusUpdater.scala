package services.orders

import scala.concurrent.ExecutionContext

import cats.data.Xor
import models.Order.{Canceled, _}
import models.{Order, OrderLineItem, OrderLineItems, Orders}
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import responses.{FullOrder, ResponseWithFailuresAndMetadata}
import services.{Result, StatusTransitionNotAllowed, NotFoundFailure400, LockedFailure, Failures}
import services.orders.Helpers._
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._
import utils.Slick.{DbResult, _}
import utils.DbResultT.*
import utils.DbResultT.implicits._

object OrderStatusUpdater {

  def updateStatus(refNum: String, newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = (for {

    order     ← * <~ mustFindOrderByRefNum(refNum)
    _         ← * <~ updateStatusesDbio(Seq(refNum), newStatus)
    updated   ← * <~ mustFindOrderByRefNum(refNum)
    response  ← * <~ FullOrder.fromOrder(updated).toXor
  } yield response).value.transactionally.run()

  // TODO: transfer sorting-paging metadata
  def updateStatuses(refNumbers: Seq[String], newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[BulkOrderUpdateResponse] = {
    updateStatusesDbio(refNumbers, newStatus).zip(OrderQueries.findAll.result).map { case (failures, orders) ⇒
      ResponseWithFailuresAndMetadata.fromXor(orders, failures.swap.toOption.map(_.toList).getOrElse(Seq.empty))
    }.transactionally.run()
  }

  private def updateStatusesDbio(refNumbers: Seq[String], newStatus: Order.Status)(implicit db: Database,
    ec: ExecutionContext, sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): DbResult[Unit] = {

    val query = Orders.filter(_.referenceNumber.inSet(refNumbers)).result
    appendForUpdate(query).flatMap { orders ⇒

      val (validTransitions, invalidTransitions) = orders
        .filterNot(_.status == newStatus)
        .partition(_.transitionAllowed(newStatus))

      val (lockedOrders, absolutelyPossibleUpdates) = validTransitions.partition(_.locked)

      updateQueries(absolutelyPossibleUpdates.map(_.id), newStatus).flatMap { _ ⇒
        // Failure handling
        val invalid = invalidTransitions.map { order ⇒
          StatusTransitionNotAllowed(order.status, newStatus, order.refNum)
        }
        val notFound = refNumbers
          .filterNot(refNum ⇒ orders.map(_.referenceNumber).contains(refNum))
          .map(refNum ⇒ NotFoundFailure400(Order, refNum))
        val locked = lockedOrders.map { order ⇒ LockedFailure(Order, order.refNum) }

        val failures = invalid ++ notFound ++ locked
        if (failures.isEmpty) DbResult.unit else DbResult.failures(Failures(failures: _*))
      }
    }
  }

  private def updateQueries(orderIds: Seq[Int], newStatus: Status) = newStatus match {
    case Canceled ⇒ cancelOrders(orderIds)
    case _ ⇒ Orders.filter(_.id.inSet(orderIds)).map(_.status).update(newStatus)
  }

  private def cancelOrders(orderIds: Seq[Int]) = {
    val updateLineItems = OrderLineItems
      .filter(_.orderId.inSetBind(orderIds))
      .map(_.status)
      .update(OrderLineItem.Canceled)

    // TODO: canceling an order must cascade to status on each payment type not order_payments
    //      val updateOrderPayments = OrderPayments
    //        .filter(_.orderId.inSetBind(orderIds))
    //        .map(_.status)
    //        .update("cancelAuth")

    val updateOrder = Orders.filter(_.id.inSetBind(orderIds)).map(_.status).update(Canceled)

    // (updateLineItems >> updateOrderPayments >> updateOrder).transactionally
    (updateLineItems >> updateOrder).transactionally
  }

}
