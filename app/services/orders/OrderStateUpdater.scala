package services.orders

import models.order.lineitems.{OrderLineItem, OrderLineItems}
import models.order.{Order, Orders}
import Order.{Canceled, _}
import failures.LockFailures.LockedFailure
import failures.{NotFoundFailure400, StateTransitionNotAllowed}
import models.StoreAdmin
import responses.BatchResponse
import responses.order.{AllOrders, FullOrder}
import responses.{BatchMetadata, BatchMetadataSource}
import services.Result
import services.LogActivity.{orderBulkStateChanged, orderStateChanged}
import slick.driver.PostgresDriver.api._
import utils.http.CustomDirectives
import utils.http.CustomDirectives.SortAndPage
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object OrderStateUpdater {

  def updateState(admin: StoreAdmin, refNum: String, newState: Order.State)
    (implicit ec: EC, db: DB, ac: AC): Result[FullOrder.Root] = (for {

    order     ← * <~ Orders.mustFindByRefNum(refNum)
    _         ← * <~ order.transitionState(newState)
    _         ← * <~ updateQueries(admin, Seq(order.id), Seq(refNum), newState).toXor
    updated   ← * <~ Orders.mustFindByRefNum(refNum)
    response  ← * <~ FullOrder.fromOrder(updated).toXor
    _         ← * <~ (if (order.state == newState) DbResult.unit else orderStateChanged(admin, response, order.state))
  } yield response).runTxn()

  // TODO: transfer sorting-paging metadata
  def updateStates(admin: StoreAdmin, refNumbers: Seq[String], newState: Order.State, skipActivity: Boolean = false)
    (implicit ec: EC, db: DB, sortAndPage: SortAndPage, ac: AC): Result[BatchResponse[AllOrders.Root]] = (for {
    // Turn failures into errors
    batchMetadata ← * <~ updateStatesDbio(admin, refNumbers, newState, skipActivity)
    response      ← * <~ OrderQueries.findAllByQuery(Orders.filter(_.referenceNumber.inSetBind(refNumbers)))
  } yield response.copy(errors = batchMetadata.flatten, batch = Some(batchMetadata))).runTxn()

  private def updateStatesDbio(admin: StoreAdmin, refNumbers: Seq[String], newState: Order.State, skipActivity: Boolean = false)
    (implicit ec: EC, ac: AC): DbResult[BatchMetadata] = {

    val query = Orders.filter(_.referenceNumber.inSet(refNumbers)).result
    appendForUpdate(query).flatMap { orders ⇒

      val (validTransitions, invalidTransitions) = orders
        .filterNot(_.state == newState)
        .partition(_.transitionAllowed(newState))

      val (lockedOrders, absolutelyPossibleUpdates) = validTransitions.partition(_.isLocked)
      val possibleIds     = absolutelyPossibleUpdates.map(_.id)
      val possibleRefNums = absolutelyPossibleUpdates.map(_.referenceNumber)
      val skipActivityMod = skipActivity || possibleRefNums.isEmpty

      updateQueriesWrapper(admin, possibleIds, possibleRefNums, newState, skipActivityMod).flatMap { _ ⇒
        // Failure handling
        val invalid = invalidTransitions
          .map(o ⇒ (o.refNum, StateTransitionNotAllowed(o.state, newState, o.refNum).description))
        val notFound = refNumbers
          .filterNot(refNum ⇒ orders.map(_.referenceNumber).contains(refNum))
          .map(refNum ⇒ (refNum, NotFoundFailure400(Order, refNum).description))
        val locked = lockedOrders.map { o ⇒ (o.refNum, LockedFailure(Order, o.refNum).description) }

        val batchFailures = (invalid ++ notFound ++ locked).toMap
        DbResult.good(BatchMetadata(BatchMetadataSource(Order, possibleRefNums, batchFailures)))
      }
    }
  }

  private def updateQueriesWrapper(admin: StoreAdmin, orderIds: Seq[Int], orderRefNums: Seq[String], newState: State,
    skipActivity: Boolean = false)(implicit ec: EC, ac: AC) = {

    if (skipActivity)
        updateQueries(admin, orderIds, orderRefNums, newState)
    else
      orderBulkStateChanged(admin, newState, orderRefNums) >>
        updateQueries(admin, orderIds, orderRefNums, newState)
  }

  private def updateQueries(admin: StoreAdmin, orderIds: Seq[Int], orderRefNums: Seq[String], newState: State) = newState match {
      case Canceled ⇒
        cancelOrders(orderIds)
      case _ ⇒
        Orders.filter(_.id.inSet(orderIds)).map(_.state).update(newState)
  }

  private def cancelOrders(orderIds: Seq[Int]) = {
    val updateLineItems = OrderLineItems
      .filter(_.orderId.inSetBind(orderIds))
      .map(_.state)
      .update(OrderLineItem.Canceled)

    // TODO: canceling an order must cascade to state on each payment type not order_payments
    //      val updateOrderPayments = OrderPayments
    //        .filter(_.orderId.inSetBind(orderIds))
    //        .map(_.state)
    //        .update("cancelAuth")

    val updateOrder = Orders.filter(_.id.inSetBind(orderIds)).map(_.state).update(Canceled)

    // (updateLineItems >> updateOrderPayments >> updateOrder).transactionally
    (updateLineItems >> updateOrder).transactionally
  }

}
