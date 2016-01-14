package services.orders

import scala.concurrent.ExecutionContext
import models.activity.ActivityContext

import models.Order.{Canceled, _}
import models.{StoreAdmin, Order, OrderLineItem, OrderLineItems, Orders}
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import responses.{FullOrder, ResponseWithFailuresAndMetadata}
import services.{LogActivity, Result, StatusTransitionNotAllowed, NotFoundFailure400, LockedFailure, Failures}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._
import utils.Slick.{DbResult, _}
import utils.DbResultT._
import utils.DbResultT.implicits._

object OrderStatusUpdater {

  def updateStatus(admin: StoreAdmin, refNum: String, newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[FullOrder.Root] = (for {

    order     ← * <~ Orders.mustFindByRefNum(refNum)
    _         ← * <~ updateStatusesDbio(admin, Seq(refNum), newStatus, skipActivity = true)
    updated   ← * <~ Orders.mustFindByRefNum(refNum)
    response  ← * <~ FullOrder.fromOrder(updated).toXor
    _         ← * <~ LogActivity.orderStateChanged(admin, response, order.status)
  } yield response).runTxn()

  // TODO: transfer sorting-paging metadata
  def updateStatuses(admin: StoreAdmin, refNumbers: Seq[String], newStatus: Order.Status, skipActivity: Boolean = false)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage, ac: ActivityContext): Result[BulkOrderUpdateResponse] = {
    updateStatusesDbio(admin, refNumbers, newStatus, skipActivity).zip(OrderQueries.findAll.result).map { case (failures, orders) ⇒
      ResponseWithFailuresAndMetadata.fromXor(orders, failures.swap.toOption.map(_.toList).getOrElse(Seq.empty))
    }.transactionally.run()
  }

  private def updateStatusesDbio(admin: StoreAdmin, refNumbers: Seq[String], newStatus: Order.Status, skipActivity: Boolean = false)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage,
      ac: ActivityContext): DbResult[Unit] = {

    val query = Orders.filter(_.referenceNumber.inSet(refNumbers)).result
    appendForUpdate(query).flatMap { orders ⇒

      val (validTransitions, invalidTransitions) = orders
        .filterNot(_.status == newStatus)
        .partition(_.transitionAllowed(newStatus))

      val (lockedOrders, absolutelyPossibleUpdates) = validTransitions.partition(_.isLocked)
      val orderIds      = absolutelyPossibleUpdates.map(_.id)
      val orderRefNums  = absolutelyPossibleUpdates.map(_.referenceNumber)

      updateQueriesWrapper(admin, orderIds, orderRefNums, newStatus, skipActivity).flatMap { _ ⇒
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

  private def updateQueriesWrapper(admin: StoreAdmin, orderIds: Seq[Int], orderRefNums: Seq[String], newStatus: Status,
    skipActivity: Boolean = false)(implicit db: Database, ec: ExecutionContext, ac: ActivityContext) = {

    if (skipActivity)
        updateQueries(admin, orderIds, orderRefNums, newStatus)
    else
      LogActivity.orderBulkStateChanged(admin, newStatus, orderRefNums) >>
        updateQueries(admin, orderIds, orderRefNums, newStatus)
  }

  private def updateQueries(admin: StoreAdmin, orderIds: Seq[Int], orderRefNums: Seq[String], newStatus: Status)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext) = newStatus match {
      case Canceled ⇒
        cancelOrders(orderIds)
      case _ ⇒
        Orders.filter(_.id.inSet(orderIds)).map(_.status).update(newStatus)
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
