package services

import scala.concurrent.ExecutionContext

import cats.data.Xor
import models.{PaymentMethod, CreditCard, Orders, Order, OrderPayment, OrderPayments, GiftCards, GiftCard,
StoreCredits, StoreCredit, CreditCards}

import payloads.{GiftCardPayment, StoreCreditPayment}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

object OrderPaymentUpdater {
  def addGiftCard(refNum: String, payload: GiftCardPayment)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {
    db.run(for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      giftCard ← GiftCards.findByCode(payload.code).result.headOption
    } yield (order, giftCard)).flatMap {
      case (Some(order), Some(giftCard)) ⇒
        if (!giftCard.isActive) {
          Result.left(GiftCardIsInactive(giftCard))
        } else if (giftCard.hasAvailable(payload.amount)) {
          val payment = OrderPayment.build(giftCard).copy(orderId = order.id, amount = Some(payload.amount))
          OrderPayments.save(payment).run().map(_ ⇒ Xor.right({}))
        } else {
          Result.left(GiftCardNotEnoughBalance(giftCard, payload.amount))
        }
      case (None, _) ⇒
        Result.left(OrderNotFoundFailure(refNum))
      case (_, None) ⇒
        Result.left(GiftCardNotFoundFailure(payload.code))
    }
  }

  def addStoreCredit(refNum: String, payload: StoreCreditPayment)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {
    db.run(for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      storeCredits ← order.map { o ⇒
        StoreCredits.findAllActiveByCustomerId(o.customerId).result
      }.getOrElse(DBIO.successful(Seq.empty[StoreCredit]))
    } yield (order, storeCredits)).flatMap {
      case (Some(order), storeCredits) ⇒
        val available = storeCredits.map(_.availableBalance).sum

        if (available < payload.amount) {
          val error = CustomerHasInsufficientStoreCredit(id = order.customerId, has = available, want = payload.amount)
          Result.left(error)
        } else {
          val payments = StoreCredit.processFifo(storeCredits.toList, payload.amount).map { case (sc, amount) ⇒
            OrderPayment.build(sc).copy(orderId = order.id, amount = Some(amount))
          }

          db.run(OrderPayments ++= payments).map(_ ⇒ Xor.right({}))
        }

      case (None, _) ⇒
        Result.left(OrderNotFoundFailure(refNum))
    }
  }

  def addCreditCard(refNum: String, id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {
    val actions = for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      creditCard ← CreditCards._findById(id).result.headOption
    } yield (order, creditCard)

    actions.run().flatMap {
      case (Some(order), Some(creditCard)) ⇒
        if (creditCard.isActive) {
          val payment = OrderPayment.build(creditCard).copy(orderId = order.id, amount = None)
          val delete = OrderPayments.creditCards.filter(_.orderId === order.id).delete
          val replaceOrCreate = OrderPayments.save(payment)

          (delete >> replaceOrCreate).transactionally.run().map(_ ⇒ Xor.right({}))
        } else {
          Result.left(CannotUseInactiveCreditCard(creditCard))
        }

      case (None, _) ⇒
        Result.left(OrderNotFoundFailure(refNum))

      case (_, None) ⇒
        Result.left(NotFoundFailure(CreditCard, id))
    }
  }

  def deleteCreditCard(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Unit] =
    deleteCreditCardOrStoreCredit(refNum, PaymentMethod.CreditCard)

  def deleteStoreCredit(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Unit] =
    deleteCreditCardOrStoreCredit(refNum, PaymentMethod.StoreCredit)

  private def deleteCreditCardOrStoreCredit(refNum: String, pmt: PaymentMethod.Type)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val actions = for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      payments ← order.map { o ⇒
        OrderPayments.byType(pmt).filter(_.orderId === o.id).delete
      }.getOrElse(DBIO.successful(0))
    } yield (order, payments)

    db.run(actions.transactionally).flatMap {
      case (None, _)        ⇒ Result.failure(OrderNotFoundFailure(refNum))
      case (Some(order), 0) ⇒ Result.failure(OrderPaymentNotFoundFailure(pmt))
      case (Some(order), _) ⇒ Result.good({})
    }
  }

  def deleteGiftCard(refNum: String, code: String)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val finders = for {
      order     ← Orders.findCartByRefNum(refNum).result.headOption
      giftCard  ← GiftCards.findByCode(code).result.headOption
    } yield (order, giftCard)

    def deletePayment(f: (Option[Order], Option[GiftCard])): DBIO[Failures Xor Unit] = f match {
      case (Some(order), Some(giftCard)) ⇒
        OrderPayments.giftCards.filter(_.paymentMethodId === giftCard.id)
          .filter(_.orderId === order.id).delete.map { rows ⇒
          if (rows == 1) Xor.right({}) else Xor.left(OrderPaymentNotFoundFailure(GiftCard).single)
        }

      case (None, _) ⇒
        DBIO.successful(Xor.left(OrderNotFoundFailure(refNum).single))

      case (_, None) ⇒
        DBIO.successful(Xor.left(GiftCardNotFoundFailure(code).single))
    }

    db.run(finders.flatMap(deletePayment).transactionally)
  }

}
