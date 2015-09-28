package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import models.{PaymentMethod, CreditCard, Orders, OrderPayment, OrderPayments, GiftCards, StoreCredits, StoreCredit,
CreditCards}
import models.OrderPayments.scope._
import payloads.{GiftCardPayment, StoreCreditPayment}
import responses.FullOrder
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick.implicits._

object OrderPaymentUpdater {
  def addGiftCard(refNum: String, payload: GiftCardPayment)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findCartByRefNum(refNum)
    finder.findOneAndRun { order ⇒
      GiftCards.findByCode(payload.code).one.flatMap {

        case Some(gc) if gc.isActive ⇒
          if (gc.hasAvailable(payload.amount)) {
            val payment = OrderPayment.build(gc).copy(orderId = order.id, amount = Some(payload.amount))
            DbResult.fromDbio(OrderPayments.save(payment) >> finder.result.head.flatMap(FullOrder.fromOrder))
          } else {
            DbResult.failure(GiftCardNotEnoughBalance(gc, payload.amount))
          }

        case Some(gc) if !gc.isActive ⇒
          DbResult.failure(GiftCardIsInactive(gc))

        case None ⇒
          DbResult.failure(GiftCardNotFoundFailure(payload.code))
      }
    }
  }

  def addStoreCredit(refNum: String, payload: StoreCreditPayment)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    db.run(for {
      order ← Orders.findCartByRefNum(refNum).one
      storeCredits ← order.map { o ⇒
        StoreCredits.findAllActiveByCustomerId(o.customerId).result
      }.getOrElse(DBIO.successful(Seq.empty[StoreCredit]))
    } yield (order, storeCredits)).flatMap {

      case (Some(order), storeCredits) ⇒
        val available = storeCredits.map(_.availableBalance).sum

        if (available < payload.amount) {
          val error = CustomerHasInsufficientStoreCredit(id = order.customerId, has = available, want = payload.amount)
          Result.left(error.single)
        } else {
          val delete = OrderPayments.filter(_.orderId === order.id).storeCredits.delete
          val payments = StoreCredit.processFifo(storeCredits.toList, payload.amount).map { case (sc, amount) ⇒
            OrderPayment.build(sc).copy(orderId = order.id, amount = Some(amount))
          }

          val queries = (delete >> (OrderPayments ++= payments)).transactionally
          db.run(queries).map(_ ⇒ Xor.right(Unit))
        }

      case (None, _) ⇒
        Result.left(OrderNotFoundFailure(refNum).single)
    }
  }

  def addCreditCard(refNum: String, id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findCartByRefNum(refNum)
    finder.findOneAndRun { order ⇒

      CreditCards._findById(id).extract.one.flatMap {
        case Some(cc) if cc.inWallet ⇒
          val payment = OrderPayment.build(cc).copy(orderId = order.id, amount = None)
          val delete = OrderPayments.filter(_.orderId === order.id).creditCards.delete

          DbResult.fromDbio(delete >> OrderPayments.save(payment) >> finder.result.head.flatMap(FullOrder.fromOrder))

        case Some(cc) ⇒
          DbResult.failure(CannotUseInactiveCreditCard(cc))

        case None ⇒
          DbResult.failure(NotFoundFailure(CreditCard, id))
      }
    }
  }

  def deleteCreditCard(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] =
    deleteCreditCardOrStoreCredit(refNum, PaymentMethod.CreditCard)

  def deleteStoreCredit(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] =
    deleteCreditCardOrStoreCredit(refNum, PaymentMethod.StoreCredit)

  private def deleteCreditCardOrStoreCredit(refNum: String, pmt: PaymentMethod.Type)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    val finder = Orders.findCartByRefNum(refNum)
    finder.findOneAndRun { order ⇒
      OrderPayments
        .filter(_.orderId === order.id)
        .byType(pmt).delete
        .flatMap(fullOrderOrFailure(_, pmt, finder))
    }
  }

  def deleteGiftCard(refNum: String, code: String)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    val finder = Orders.findCartByRefNum(refNum)
    finder.findOneAndRun { order ⇒
      GiftCards.findByCode(code).one.flatMap {

        case Some(giftCard) ⇒
          OrderPayments
            .filter(_.paymentMethodId === giftCard.id)
            .filter(_.orderId === order.id)
            .giftCards.delete
            .flatMap(fullOrderOrFailure(_, PaymentMethod.GiftCard, finder))

        case None ⇒
          DbResult.failure(GiftCardNotFoundFailure(code))
      }
    }
  }

  private def fullOrderOrFailure(rowsDeleted: Int, pmt: PaymentMethod.Type, finder: Orders.QuerySeq)
    (implicit ec: ExecutionContext, db: Database): DbResult[FullOrder.Root] = {
    if (rowsDeleted == 0)
      DbResult.failure(OrderPaymentNotFoundFailure(pmt))
    else
      DbResult.fromDbio(finder.result.head.flatMap(FullOrder.fromOrder))
  }
}
