package services.orders

import cats.implicits._
import failures.{NotFoundFailure400, StateTransitionNotAllowed}
import models.account._
import models.cord.Order._
import models.cord._
import models.cord.lineitems._
import models.payment.giftcard.GiftCardAdjustments
import models.payment.giftcard.GiftCardAdjustments.scope._
import models.payment.storecredit.StoreCreditAdjustments
import models.payment.storecredit.StoreCreditAdjustments.scope._
import responses.cord.{AllOrders, OrderResponse}
import responses.{BatchMetadata, BatchMetadataSource, BatchResponse}
import services.LogActivity.{orderBulkStateChanged, orderStateChanged}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object OrderStateUpdater {

  def updateState(admin: User, refNum: String, newState: Order.State)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[OrderResponse] =
    for {
      order    ← * <~ Orders.mustFindByRefNum(refNum)
      _        ← * <~ order.transitionState(newState)
      _        ← * <~ updateQueries(admin, Seq(refNum), newState)
      updated  ← * <~ Orders.mustFindByRefNum(refNum)
      response ← * <~ OrderResponse.fromOrder(updated, grouped = true)
      _        ← * <~ doOrMeh(order.state != newState, orderStateChanged(admin, response, order.state))
    } yield response

  def updateStates(admin: User,
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
      admin: User,
      refNumbers: Seq[String],
      newState: Order.State,
      skipActivity: Boolean = false)(implicit ec: EC, ac: AC, db: DB): DbResultT[BatchMetadata] = {

    val query = Orders.filter(_.referenceNumber.inSet(refNumbers)).result
    appendForUpdate(query).dbresult.flatMap { orders ⇒
      val (validTransitions, invalidTransitions) =
        orders.filterNot(_.state == newState).partition(_.transitionAllowed(newState))

      val possibleRefNums = validTransitions.map(_.referenceNumber)
      val skipActivityMod = skipActivity || possibleRefNums.isEmpty

      updateQueriesWrapper(admin, possibleRefNums, newState, skipActivityMod).flatMap { _ ⇒
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

  private def updateQueriesWrapper(
      admin: User,
      cordRefs: Seq[String],
      newState: State,
      skipActivity: Boolean = false)(implicit ec: EC, ac: AC, db: DB): DbResultT[Unit] = {

    if (skipActivity)
      updateQueries(admin, cordRefs, newState)
    else
      for {
        _ ← * <~ orderBulkStateChanged(newState, cordRefs, admin.some)
        _ ← * <~ updateQueries(admin, cordRefs, newState)
      } yield ()
  }

  private def updateQueries(admin: User, cordRefs: Seq[String], newState: State)(
      implicit ec: EC,
      db: DB): DbResultT[Unit] =
    newState match {
      case Canceled ⇒
        cancelOrders(cordRefs)
      case _ ⇒
        // FIXME: calling .dbresultt (which basically maps right) can be dangerous here. @anna
        Orders.filter(_.referenceNumber.inSet(cordRefs)).map(_.state).update(newState).dbresult.meh
    }

  private def cancelOrders(cordRefs: Seq[String])(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      updateLineItems ← * <~ OrderLineItems
                         .filter(_.cordRef.inSetBind(cordRefs))
                         .map(_.state)
                         .update(OrderLineItem.Canceled)

      orderPayments ← * <~ OrderPayments.filter(_.cordRef.inSetBind(cordRefs)).result
      _             ← * <~ cancelGiftCards(orderPayments)
      _             ← * <~ cancelStoreCredits(orderPayments)
      _             ← * <~ Orders.filter(_.referenceNumber.inSetBind(cordRefs)).map(_.state).update(Canceled)
    } yield ()

  private def cancelGiftCards(orderPayments: Seq[OrderPayment])(implicit ec: EC,
                                                                db: DB): DBIO[Int] = {
    val paymentIds = orderPayments.map(_.id)
    GiftCardAdjustments.filter(_.orderPaymentId.inSetBind(paymentIds)).cancel()
  }

  private def cancelStoreCredits(orderPayments: Seq[OrderPayment])(implicit ec: EC,
                                                                   db: DB): DBIO[Int] = {
    val paymentIds = orderPayments.map(_.id)
    StoreCreditAdjustments.filter(_.orderPaymentId.inSetBind(paymentIds)).cancel()
  }
}
