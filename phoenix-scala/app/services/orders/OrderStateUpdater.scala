package services.orders

import failures.{NotFoundFailure400, StateTransitionNotAllowed}
import models.StoreAdmin
import models.cord.Order._
import models.cord._
import models.cord.lineitems._
import models.payment.giftcard.GiftCardAdjustments
import models.payment.giftcard.GiftCardAdjustments.scope._
import models.payment.storecredit.StoreCreditAdjustments
import models.payment.storecredit.StoreCreditAdjustments.scope._
import responses.order.{AllOrders, FullOrder}
import responses.{BatchMetadata, BatchMetadataSource, BatchResponse}
import services.LogActivity.{orderBulkStateChanged, orderStateChanged}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object OrderStateUpdater {

  def updateState(admin: StoreAdmin, refNum: String, newState: Order.State)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[FullOrder.Root] =
    for {
      order    ← * <~ Orders.mustFindByRefNum(refNum)
      _        ← * <~ order.transitionState(newState)
      _        ← * <~ updateQueries(admin, Seq(refNum), newState)
      updated  ← * <~ Orders.mustFindByRefNum(refNum)
      response ← * <~ FullOrder.fromOrder(updated)
      _ ← * <~ (if (order.state == newState) DbResultT.unit
                else orderStateChanged(admin, response, order.state))
    } yield response

  def updateStates(admin: StoreAdmin,
                   refNumbers: Seq[String],
                   newState: Order.State,
                   skipActivity: Boolean = false)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[BatchResponse[AllOrders.Root]] =
    for {
      // Turn failures into errors
      batchMetadata ← * <~ updateStatesDbio(admin, refNumbers, newState, skipActivity)
      response ← * <~ OrderQueries.findAllByQuery(
                    Orders.filter(_.referenceNumber.inSetBind(refNumbers)))
    } yield response.copy(errors = batchMetadata.flatten, batch = Some(batchMetadata))

  private def updateStatesDbio(
      admin: StoreAdmin,
      refNumbers: Seq[String],
      newState: Order.State,
      skipActivity: Boolean = false)(implicit ec: EC, ac: AC): DbResultT[BatchMetadata] = {

    val query = Orders.filter(_.referenceNumber.inSet(refNumbers)).result
    appendForUpdate(query).toXor.flatMap { orders ⇒
      val (validTransitions, invalidTransitions) =
        orders.filterNot(_.state == newState).partition(_.transitionAllowed(newState))

      val possibleRefNums = validTransitions.map(_.referenceNumber)
      val skipActivityMod = skipActivity || possibleRefNums.isEmpty

      updateQueriesWrapper(admin, possibleRefNums, newState, skipActivityMod).toXor.flatMap { _ ⇒
        // Failure handling
        val invalid = invalidTransitions.map(o ⇒
              (o.refNum, StateTransitionNotAllowed(o.state, newState, o.refNum).description))
        val notFound = refNumbers
          .filterNot(refNum ⇒ orders.map(_.referenceNumber).contains(refNum))
          .map(refNum ⇒ (refNum, NotFoundFailure400(Order, refNum).description))

        val batchFailures = (invalid ++ notFound).toMap
        DbResultT.good(BatchMetadata(BatchMetadataSource(Order, possibleRefNums, batchFailures)))
      }
    }
  }

  private def updateQueriesWrapper(admin: StoreAdmin,
                                   cordRefs: Seq[String],
                                   newState: State,
                                   skipActivity: Boolean = false)(implicit ec: EC, ac: AC) = {

    if (skipActivity)
      updateQueries(admin, cordRefs, newState)
    else
      orderBulkStateChanged(admin, newState, cordRefs).value >>
      updateQueries(admin, cordRefs, newState)
  }

  private def updateQueries(admin: StoreAdmin, cordRefs: Seq[String], newState: State)(
      implicit ec: EC) =
    newState match {
      case Canceled ⇒
        cancelOrders(cordRefs)
      case _ ⇒
        Orders.filter(_.referenceNumber.inSet(cordRefs)).map(_.state).update(newState)
    }

  private def cancelOrders(cordRefs: Seq[String])(implicit ec: EC) = {
    val updateLineItems = OrderLineItems
      .filter(_.cordRef.inSetBind(cordRefs))
      .map(_.state)
      .update(OrderLineItem.Canceled)

    val cancelPayments = for {
      orderPayments ← OrderPayments.filter(_.cordRef.inSetBind(cordRefs)).result
      _             ← cancelGiftCards(orderPayments)
      _             ← cancelStoreCredits(orderPayments)
      // TODO: add credit card charge return
    } yield ()

    val updateOrder =
      Orders.filter(_.referenceNumber.inSetBind(cordRefs)).map(_.state).update(Canceled)

    // (updateLineItems >> updateOrderPayments >> updateOrder).transactionally
    (updateLineItems >> updateOrder >> cancelPayments).transactionally
  }

  private def cancelGiftCards(orderPayments: Seq[OrderPayment]) = {
    val paymentIds = orderPayments.map(_.id)
    GiftCardAdjustments.filter(_.orderPaymentId.inSetBind(paymentIds)).cancel()
  }

  private def cancelStoreCredits(orderPayments: Seq[OrderPayment]) = {
    val paymentIds = orderPayments.map(_.id)
    StoreCreditAdjustments.filter(_.orderPaymentId.inSetBind(paymentIds)).cancel()
  }
}
