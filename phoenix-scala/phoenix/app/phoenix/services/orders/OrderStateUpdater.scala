package phoenix.services.orders

import cats.implicits._
import core.db._
import core.failures.NotFoundFailure400
import phoenix.failures.StateTransitionNotAllowed
import phoenix.models.account._
import phoenix.models.cord.Order._
import phoenix.models.cord._
import phoenix.models.cord.lineitems._
import phoenix.models.payment.giftcard.GiftCardAdjustments
import phoenix.models.payment.giftcard.GiftCardAdjustments.scope._
import phoenix.models.payment.storecredit.StoreCreditAdjustments
import phoenix.models.payment.storecredit.StoreCreditAdjustments.scope._
import phoenix.responses.cord.{AllOrders, OrderResponse}
import phoenix.responses.{BatchMetadata, BatchMetadataSource}
import phoenix.services.LogActivity
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import responses.BatchResponse
import slick.jdbc.PostgresProfile.api._

object OrderStateUpdater {

  def updateState(
      admin: User,
      refNum: String,
      newState: Order.State)(implicit ec: EC, db: DB, ac: AC, apis: Apis, au: AU): DbResultT[OrderResponse] =
    for {
      order   ← * <~ Orders.mustFindByRefNum(refNum)
      _       ← * <~ order.transitionState(newState)
      _       ← * <~ updateQueries(admin, Seq(refNum), newState)
      updated ← * <~ Orders.mustFindByRefNum(refNum)
      _ ← * <~ when(updated.state == Order.Canceled,
                    DbResultT.fromResult(apis.middlewarehouse.cancelHold(refNum)))
      response ← * <~ OrderResponse.fromOrder(updated, grouped = true)
      _ ← * <~ when(order.state != newState,
                    LogActivity().orderStateChanged(admin, response, order.state).void)
    } yield response

  def updateStates(
      admin: User,
      refNumbers: Seq[String],
      newState: Order.State,
      skipActivity: Boolean = false)(implicit ec: EC, db: DB, ac: AC): DbResultT[BatchResponse[AllOrders]] =
    for {
      // Turn failures into errors
      batchMetadata ← * <~ updateStatesDbio(admin, refNumbers, newState, skipActivity)
      response      ← * <~ OrderQueries.findAllByQuery(Orders.filter(_.referenceNumber.inSetBind(refNumbers)))
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
        BatchMetadata(BatchMetadataSource(Order, possibleRefNums, batchFailures)).pure[DbResultT]
      }
    }
  }

  private def updateQueriesWrapper(
      admin: User,
      cordRefs: Seq[String],
      newState: State,
      skipActivity: Boolean = false)(implicit ec: EC, ac: AC, db: DB): DbResultT[Unit] =
    if (skipActivity)
      updateQueries(admin, cordRefs, newState)
    else
      for {
        _ ← * <~ LogActivity().orderBulkStateChanged(newState, cordRefs, admin.some)
        _ ← * <~ updateQueries(admin, cordRefs, newState)
      } yield ()

  private def updateQueries(admin: User, cordRefs: Seq[String], newState: State)(implicit ec: EC,
                                                                                 db: DB): DbResultT[Unit] =
    newState match {
      case Canceled ⇒
        cancelOrders(cordRefs)
      case _ ⇒
        // FIXME: calling .dbresultt (which basically maps right) can be dangerous here. @anna
        Orders.filter(_.referenceNumber.inSet(cordRefs)).map(_.state).update(newState).dbresult.void
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

  private def cancelGiftCards(orderPayments: Seq[OrderPayment])(implicit ec: EC, db: DB): DBIO[Int] = {
    val paymentIds = orderPayments.map(_.id)
    GiftCardAdjustments.filter(_.orderPaymentId.inSetBind(paymentIds)).cancel()
  }

  private def cancelStoreCredits(orderPayments: Seq[OrderPayment])(implicit ec: EC, db: DB): DBIO[Int] = {
    val paymentIds = orderPayments.map(_.id)
    StoreCreditAdjustments.filter(_.orderPaymentId.inSetBind(paymentIds)).cancel()
  }
}
