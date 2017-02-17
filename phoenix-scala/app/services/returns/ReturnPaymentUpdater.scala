package services.returns

import failures.OrderFailures.OrderPaymentNotFoundFailure
import models.account.Scope
import models.cord.{Cart, OrderPayment, OrderPayments}
import models.cord.OrderPayments.scope._
import models.payment.PaymentMethod
import models.payment.creditcard.CreditCards
import models.payment.giftcard._
import models.payment.storecredit._
import models.returns.ReturnPayments.scope._
import models.returns._
import payloads.ReturnPayloads.ReturnPaymentPayload
import responses.ReturnResponse
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ReturnPaymentUpdater {
  def addPayment(refNum: String, payload: ReturnPaymentPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[ReturnResponse.Root] =
    for {
      rma      ← * <~ Returns.mustFindPendingByRefNum404(refNum)
      payment  ← * <~ mustFindCcPaymentsByOrderRef(rma.orderRef)
      _        ← * <~ processAddPayment(rma, payment, payload)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(rma)
    } yield response

  private def mustFindCcPaymentsByOrderRef(cordRef: String)(
      implicit ec: EC): DbResultT[OrderPayment] =
    OrderPayments
      .findAllByCordRef(cordRef)
      .creditCards
      .mustFindOneOr(OrderPaymentNotFoundFailure(Cart))

  private def processAddPayment(rma: Return, payment: OrderPayment, payload: ReturnPaymentPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[ReturnPayment] = payload.method match {
    case PaymentMethod.CreditCard ⇒ addCreditCard(rma.id, payment, payload.amount)
    case PaymentMethod.GiftCard   ⇒ addGiftCard(rma.id, payment, payload.amount)
    case PaymentMethod.StoreCredit ⇒
      addStoreCredit(returnId = rma.id, accountId = rma.accountId, payment, payload.amount)
  }

  private def addCreditCard(returnId: Int, payment: OrderPayment, amount: Int)(
      implicit ec: EC,
      db: DB): DbResultT[ReturnPayment] =
    for {
      cc ← * <~ CreditCards.mustFindById404(payment.paymentMethodId)
      _  ← * <~ deleteCc(returnId)
      ccRefund ← * <~ ReturnPayments.create(
                    ReturnPayment.build(cc, returnId, amount, payment.currency))
    } yield ccRefund

  private def addGiftCard(returnId: Int,
                          payment: OrderPayment,
                          amount: Int)(implicit ec: EC, db: DB, au: AU): DbResultT[ReturnPayment] =
    for {
      _      ← * <~ deleteGc(returnId)
      origin ← * <~ GiftCardRefunds.create(GiftCardRefund(returnId = returnId))
      gc     ← * <~ GiftCards.create(GiftCard.buildRmaProcess(origin.id, payment.currency))
      pmt    ← * <~ ReturnPayments.create(ReturnPayment.build(gc, returnId, amount, payment.currency))
    } yield pmt

  private def addStoreCredit(returnId: Int, accountId: Int, payment: OrderPayment, amount: Int)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[ReturnPayment] =
    for {
      _      ← * <~ deleteSc(returnId)
      origin ← * <~ StoreCreditRefunds.create(StoreCreditRefund(returnId = returnId))
      sc ← * <~ StoreCredits.create(
              StoreCredit(accountId = accountId,
                          scope = Scope.current,
                          originId = origin.id,
                          originType = StoreCredit.RmaProcess,
                          currency = payment.currency,
                          originalBalance = 0))
      pmt ← * <~ ReturnPayments.create(ReturnPayment.build(sc, returnId, amount, payment.currency))
    } yield pmt

  def deletePayment(refNum: String, paymentMethod: PaymentMethod.Type)(
      implicit ec: EC,
      db: DB): DbResultT[ReturnResponse.Root] =
    for {
      rma      ← * <~ Returns.mustFindPendingByRefNum404(refNum)
      _        ← * <~ processDeletePayment(rma.id, paymentMethod)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(rma)
    } yield response

  def processDeletePayment(returnId: Int, paymentMethod: PaymentMethod.Type)(
      implicit ec: EC): DbResultT[Unit] =
    paymentMethod match {
      case PaymentMethod.CreditCard  ⇒ deleteCc(returnId)
      case PaymentMethod.GiftCard    ⇒ deleteGc(returnId)
      case PaymentMethod.StoreCredit ⇒ deleteSc(returnId)
    }

  private def deleteCc(returnId: Int)(implicit ec: EC): DbResultT[Unit] =
    ReturnPayments.findAllByReturnId(returnId).creditCards.deleteAll.meh

  private def deleteGc(returnId: Int)(implicit ec: EC): DbResultT[Unit] =
    for {
      paymentMethodIds ← * <~ ReturnPayments.findAllPaymentIdsByReturnId(returnId).result
      giftCardOriginIds ← * <~ GiftCards
                           .findAllByIds(paymentMethodIds)
                           .map(_.originId)
                           .to[Set]
                           .result
      _ ← * <~ ReturnPayments.findAllByReturnId(returnId).deleteAll
      _ ← * <~ GiftCards.findAllByIds(paymentMethodIds).deleteAll
      _ ← * <~ GiftCardRefunds.findAllByIds(giftCardOriginIds).deleteAll
    } yield ()

  private def deleteSc(returnId: Int)(implicit ec: EC): DbResultT[Unit] =
    for {
      paymentMethodIds ← * <~ ReturnPayments.findAllPaymentIdsByReturnId(returnId).result
      storeCreditOriginIds ← * <~ StoreCredits
                              .findAllByIds(paymentMethodIds)
                              .map(_.originId)
                              .to[Set]
                              .result
      _ ← * <~ ReturnPayments.findAllByReturnId(returnId).deleteAll
      _ ← * <~ StoreCredits.findAllByIds(paymentMethodIds).deleteAll
      _ ← * <~ StoreCreditRefunds.findAllByIds(storeCreditOriginIds).deleteAll
    } yield ()
}
