package services.returns

import failures.OrderFailures.OrderPaymentNotFoundFailure
import failures.ReturnFailures.{ReturnCCPaymentExceeded, ReturnPaymentExceeded}
import models.account.Scope
import models.cord.OrderPayments.scope._
import models.cord._
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCardCharges, CreditCards}
import models.payment.giftcard._
import models.payment.storecredit._
import models.returns.ReturnPayments.scope._
import models.returns._
import payloads.ReturnPayloads.{ReturnPaymentPayload, ReturnPaymentsPayload}
import responses.ReturnResponse
import services.carts.CartTotaler
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import cats.implicits._

object ReturnPaymentUpdater {
  def addPayments(refNum: String, payload: ReturnPaymentsPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[ReturnResponse.Root] = {
    @inline
    def addPayment(rma: Return,
                   payment: OrderPayment,
                   paymentMethodAmount: (PaymentMethod.Type, Int)): DbResultT[ReturnPayment] =
      processAddPayment(rma, payment, paymentMethodAmount._1, paymentMethodAmount._2)

    val payments = payload.payments.filter { case (_, amount) ⇒ amount > 0 }
    if (payments.isEmpty)
      Returns.mustFindPendingByRefNum404(refNum).flatMap(ReturnResponse.fromRma)
    else
      for {
        rma      ← * <~ Returns.mustFindPendingByRefNum404(refNum)
        _        ← * <~ validateMaxAllowedPayments(rma, payments)
        payment  ← * <~ mustFindCcPaymentsByOrderRef(rma.orderRef)
        _        ← * <~ payments.map(addPayment(rma, payment, _)).toList
        _        ← * <~ updateTotalsReturn(rma)
        updated  ← * <~ Returns.refresh(rma)
        response ← * <~ ReturnResponse.fromRma(updated)
      } yield response
  }

  def addPayment(refNum: String, method: PaymentMethod.Type, payload: ReturnPaymentPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[ReturnResponse.Root] =
    for {
      rma      ← * <~ Returns.mustFindPendingByRefNum404(refNum)
      _        ← * <~ validateMaxAllowedPayments(rma, Map(method → payload.amount))
      payment  ← * <~ mustFindCcPaymentsByOrderRef(rma.orderRef)
      _        ← * <~ processAddPayment(rma, payment, method, payload.amount)
      _        ← * <~ updateTotalsReturn(rma)
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
    } yield response

  private[this] def updateTotalsReturn(rma: Return)(implicit ec: EC, db: DB, au: AU) =
    for {
      totalRefund ← * <~ ReturnPayments.filter(_.returnId === rma.id).map(_.amount).sum.result
      _           ← * <~ Returns.update(rma, rma.copy(totalRefund = rma.totalRefund |+| totalRefund))
    } yield ()

  private def validateMaxAllowedPayments(rma: Return, payments: Map[PaymentMethod.Type, Int])(
      implicit ec: EC,
      db: DB): DbResultT[Unit] = {
    def validateTotalPayment() =
      for {
        adjustments ← * <~ ReturnTotaler.adjustmentsTotal(rma)
        subTotal    ← * <~ ReturnTotaler.subTotal(rma)
        shipping ← * <~ ReturnLineItemShippingCosts
                    .findByRmaId(rma.id)
                    .map(_.amount)
                    .sum
                    .getOrElse(0)
                    .result
        taxes ← * <~ CartTotaler.taxesTotal(cordRef = rma.orderRef,
                                            subTotal = subTotal,
                                            shipping = shipping,
                                            adjustments = adjustments)
        maxAmount = math.max(0, subTotal + taxes + shipping - adjustments)
        amount    = payments.valuesIterator.sum

        _ ← * <~ failIf(amount > maxAmount,
                        ReturnPaymentExceeded(refNum = rma.referenceNumber,
                                              amount = amount,
                                              maxAmount = maxAmount))
      } yield ()

    def validateCCPayment() = {
      val orderCCPaymentQuery = OrderPayments
        .findAllCreditCardsForOrder(rma.orderRef)
        .join(CreditCardCharges)
        .on(_.id === _.orderPaymentId)
        .map { case (_, charge) ⇒ charge.amount }
        .sum
        .getOrElse(0)
        .result
        .dbresult
      val previousCCPaymentsQuery = Returns
        .findByOrderRefNum(rma.orderRef)
        .filter(_.id =!= rma.id)
        .join(ReturnPayments.creditCards)
        .on(_.id === _.returnId)
        .map { case (_, payment) ⇒ payment.amount }
        .sum
        .getOrElse(0)
        .result
      val ccAmount = payments.getOrElse(PaymentMethod.CreditCard, 0)

      if (ccAmount > 0)
        for {
          previousCCPayments ← * <~ previousCCPaymentsQuery
          orderCCPayment     ← * <~ orderCCPaymentQuery
          maxCCAmount = orderCCPayment - previousCCPayments
          _ ← * <~ failIf(ccAmount > maxCCAmount,
                          ReturnCCPaymentExceeded(refNum = rma.referenceNumber,
                                                  amount = ccAmount,
                                                  maxAmount = maxCCAmount))
        } yield ()
      else DbResultT.pure(())
    }

    for {
      _ ← * <~ validateTotalPayment()
      _ ← * <~ validateCCPayment()
    } yield ()
  }

  private def mustFindCcPaymentsByOrderRef(cordRef: String)(
      implicit ec: EC): DbResultT[OrderPayment] =
    OrderPayments
      .findAllByCordRef(cordRef)
      .creditCards
      .mustFindOneOr(OrderPaymentNotFoundFailure(Order))

  private def processAddPayment(
      rma: Return,
      payment: OrderPayment,
      method: PaymentMethod.Type,
      amount: Int)(implicit ec: EC, db: DB, au: AU): DbResultT[ReturnPayment] =
    method match {
      case PaymentMethod.CreditCard ⇒ addCreditCard(rma.id, payment, amount)
      case PaymentMethod.GiftCard   ⇒ addGiftCard(rma.id, payment, amount)
      case PaymentMethod.StoreCredit ⇒
        addStoreCredit(returnId = rma.id, accountId = rma.accountId, payment, amount)
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

  private def deleteGc(returnId: Int)(implicit ec: EC): DbResultT[Unit] = {
    val gcQuery = ReturnPayments.findAllByReturnId(returnId).giftCards
    for {
      paymentMethodIds ← * <~ gcQuery.paymentMethodIds.result
      giftCardOriginIds ← * <~ GiftCards
                           .findAllByIds(paymentMethodIds)
                           .map(_.originId)
                           .to[Set]
                           .result
      _ ← * <~ gcQuery.deleteAll
      _ ← * <~ GiftCards.findAllByIds(paymentMethodIds).deleteAll
      _ ← * <~ GiftCardRefunds.findAllByIds(giftCardOriginIds).deleteAll
    } yield ()
  }

  private def deleteSc(returnId: Int)(implicit ec: EC): DbResultT[Unit] = {
    val scQuery = ReturnPayments.findAllByReturnId(returnId).storeCredits
    for {
      paymentMethodIds ← * <~ scQuery.paymentMethodIds.result
      storeCreditOriginIds ← * <~ StoreCredits
                              .findAllByIds(paymentMethodIds)
                              .map(_.originId)
                              .to[Set]
                              .result
      _ ← * <~ scQuery.deleteAll
      _ ← * <~ StoreCredits.findAllByIds(paymentMethodIds).deleteAll
      _ ← * <~ StoreCreditRefunds.findAllByIds(storeCreditOriginIds).deleteAll
    } yield ()
  }
}
