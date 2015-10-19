package services

import scala.concurrent.ExecutionContext

import models.{GiftCard, PaymentMethod, CreditCard, Orders, OrderPayment, OrderPayments, GiftCards, StoreCredits,
StoreCredit, CreditCards}
import models.OrderPayments.scope._
import payloads.{GiftCardPayment, StoreCreditPayment}
import responses.FullOrder
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick._
import utils.Slick.implicits._

object OrderPaymentUpdater {
  def addGiftCard(refNum: String, payload: GiftCardPayment)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findCartByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒
      GiftCards.findByCode(payload.code).one.flatMap {

        case Some(gc) if gc.isActive ⇒
          if (gc.hasAvailable(payload.amount)) {
            val payment = OrderPayment.build(gc).copy(orderId = order.id, amount = Some(payload.amount))
            DbResult.fromDbio(OrderPayments.save(payment) >> fullOrder(finder))
          } else {
            DbResult.failure(GiftCardNotEnoughBalance(gc, payload.amount))
          }

        case Some(gc) if gc.isCart ⇒
          DbResult.failure(NotFoundFailure400(GiftCard, payload.code))

        case Some(gc) if !gc.isActive && !gc.isCart ⇒
          DbResult.failure(GiftCardIsInactive(gc))

        case None ⇒
          DbResult.failure(NotFoundFailure400(GiftCard, payload.code))
      }
    }
  }

  def addStoreCredit(refNum: String, payload: StoreCreditPayment)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    val finder = Orders.findCartByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒
      StoreCredits.findAllActiveByCustomerId(order.customerId).result.flatMap { storeCredits ⇒
        val reqAmount = payload.amount

        val available = storeCredits.map(_.availableBalance).sum

        if (available < reqAmount) {
          DbResult.failure(CustomerHasInsufficientStoreCredit(id = order.customerId, has = available, want = reqAmount))
        } else {
          val delete = OrderPayments.filter(_.orderId === order.id).storeCredits.delete
          val payments = StoreCredit.processFifo(storeCredits.toList, reqAmount).map { case (sc, amount) ⇒
            OrderPayment.build(sc).copy(orderId = order.id, amount = Some(amount))
          }

          DbResult.fromDbio(delete >> (OrderPayments ++= payments) >> fullOrder(finder))
        }
      }
    }
  }

  def addCreditCard(refNum: String, id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {
    val finder = Orders.findCartByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒

      CreditCards.findById(id).extract.one.flatMap {
        case Some(cc) if cc.inWallet ⇒
          val payment = OrderPayment.build(cc).copy(orderId = order.id, amount = None)
          val delete = OrderPayments.filter(_.orderId === order.id).creditCards.delete

          DbResult.fromDbio(delete >> OrderPayments.save(payment) >> fullOrder(finder))

        case Some(cc) ⇒
          DbResult.failure(CannotUseInactiveCreditCard(cc))

        case None ⇒
          DbResult.failure(NotFoundFailure400(CreditCard, id))
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
    finder.selectOneForUpdate { order ⇒
      OrderPayments
        .filter(_.orderId === order.id)
        .byType(pmt).delete
        .flatMap(fullOrderOrFailure(_, pmt, finder))
    }
  }

  def deleteGiftCard(refNum: String, code: String)
    (implicit ec: ExecutionContext, db: Database): Result[FullOrder.Root] = {

    val finder = Orders.findCartByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒
      GiftCards.findByCode(code).one.flatMap {

        case Some(giftCard) ⇒
          OrderPayments
            .filter(_.paymentMethodId === giftCard.id)
            .filter(_.orderId === order.id)
            .giftCards.delete
            .flatMap(fullOrderOrFailure(_, PaymentMethod.GiftCard, finder))

        case None ⇒
          DbResult.failure(NotFoundFailure404(GiftCard, code))
      }
    }
  }

  private def fullOrderOrFailure(rowsDeleted: Int, pmt: PaymentMethod.Type, finder: Orders.QuerySeq)
    (implicit ec: ExecutionContext, db: Database): DbResult[FullOrder.Root] = {
    if (rowsDeleted == 0)
      DbResult.failure(OrderPaymentNotFoundFailure(pmt))
    else
      DbResult.fromDbio(fullOrder(finder))
  }
}
