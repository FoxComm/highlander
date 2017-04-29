package services.returns

import cats.implicits._
import failures.OrderFailures.OrderPaymentNotFoundFailure
import failures.ReturnFailures._
import models.account.{Scope, User, Users}
import models.cord.OrderPayments.scope._
import models.cord._
import models.payment.PaymentMethod
import models.payment.creditcard.{CreditCardCharge, CreditCardCharges, CreditCards}
import models.payment.giftcard._
import models.payment.storecredit._
import models.returns.ReturnPayments.scope._
import models.returns._
import responses.ReturnResponse
import scala.annotation.tailrec
import services.LogActivity
import services.carts.CartTotaler
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.apis.{Apis, RefundReason}
import utils.db._

object ReturnPaymentManager {
  def updatePayments(refNum: String, payments: Map[PaymentMethod.Type, Int], overwrite: Boolean)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      au: AU): DbResultT[ReturnResponse.Root] = {

    @inline
    def addPayment(rma: Return,
                   payment: OrderPayment,
                   paymentMethodAmount: (PaymentMethod.Type, Int)) = {
      val (method, amount) = paymentMethodAmount
      processAddPayment(rma, payment, method, amount)
    }

    val paymentsToAdd         = payments.filter { case (_, amount) ⇒ amount > 0 }
    lazy val paymentsToRemove = (PaymentMethod.Type.types -- paymentsToAdd.keySet).toList

    if (paymentsToAdd.isEmpty)
      for {
        rma ← * <~ Returns.mustFindActiveByRefNum404(refNum)
        response ← * <~ ifElse(overwrite,
                               deletePayments(rma, paymentsToRemove),
                               ReturnResponse.fromRma(rma))
      } yield response
    else
      for {
        rma     ← * <~ Returns.mustFindActiveByRefNum404(refNum)
        payment ← * <~ mustFindCcPaymentsByOrderRef(rma.orderRef)
        _       ← * <~ validateMaxAllowedPayments(rma, paymentsToAdd, sumOther = !overwrite)
        _       ← * <~ paymentsToAdd.map(addPayment(rma, payment, _)).toList
        _       ← * <~ updateTotalsReturn(rma)
        response ← * <~ ifElse(overwrite,
                               deletePayments(rma, paymentsToRemove),
                               Returns.refresh(rma).dbresult.flatMap(ReturnResponse.fromRma))
        _ ← * <~ LogActivity().returnPaymentsAdded(response, paymentsToAdd.keysIterator.toList)
      } yield response
  }

  private[this] def updateTotalsReturn(rma: Return)(implicit ec: EC, db: DB, au: AU) =
    for {
      totalRefund ← * <~ ReturnPayments.findAllByReturnId(rma.id).map(_.amount).sum.result
      _           ← * <~ Returns.update(rma, rma.copy(totalRefund = rma.totalRefund |+| totalRefund))
    } yield ()

  private def validateMaxAllowedPayments(
      rma: Return,
      payments: Map[PaymentMethod.Type, Int],
      sumOther: Boolean)(implicit ec: EC, db: DB): DbResultT[Unit] = {
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
      val previousCCPaymentsQuery = Returns
        .findPrevious(rma)
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
                          ReturnCcPaymentExceeded(refNum = rma.referenceNumber,
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
      case PaymentMethod.ApplePay   ⇒ addApplePayment(rma.id, payment, amount)
      case PaymentMethod.StoreCredit ⇒
        addStoreCredit(returnId = rma.id, accountId = rma.accountId, payment, amount)
    }

  private def addCreditCard(returnId: Int, payment: OrderPayment, amount: Int)(
      implicit ec: EC,
      db: DB): DbResultT[ReturnPayment] =
    for {
      cc ← * <~ CreditCards.mustFindById404(payment.paymentMethodId)
      _  ← * <~ deleteCcPayment(returnId)
      ccRefund ← * <~ ReturnPayments.create(
                    ReturnPayment(returnId = returnId,
                                  amount = amount,
                                  currency = payment.currency,
                                  paymentMethodId = cc.id,
                                  paymentMethodType = PaymentMethod.CreditCard))
    } yield ccRefund

  private def addApplePayment(returnId: Int, payment: OrderPayment, amount: Int)(implicit ec: EC,
                                                                                 db: DB,
                                                                                 au: AU) =
    DbResultT.pure(
        ReturnPayment(
            paymentMethodId = payment.id,
            amount = amount,
            paymentMethodType = PaymentMethod.ApplePay)) // TODO implement AP returns @aafa

  private def addGiftCard(returnId: Int,
                          payment: OrderPayment,
                          amount: Int)(implicit ec: EC, db: DB, au: AU): DbResultT[ReturnPayment] =
    for {
      _      ← * <~ deleteGcPayment(returnId)
      origin ← * <~ GiftCardRefunds.create(GiftCardRefund(returnId = returnId))
      gc ← * <~ GiftCards.create(
              GiftCard(
                  scope = Scope.current,
                  originId = origin.id,
                  originType = GiftCard.RmaProcess,
                  state = GiftCard.OnHold,
                  currency = payment.currency,
                  originalBalance = amount,
                  availableBalance = amount,
                  currentBalance = amount
              ))
      pmt ← * <~ ReturnPayments.create(
               ReturnPayment(returnId = returnId,
                             amount = amount,
                             currency = payment.currency,
                             paymentMethodId = gc.id,
                             paymentMethodType = PaymentMethod.GiftCard))
    } yield pmt

  private def addStoreCredit(returnId: Int, accountId: Int, payment: OrderPayment, amount: Int)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[ReturnPayment] =
    for {
      _      ← * <~ deleteScPayment(returnId)
      origin ← * <~ StoreCreditRefunds.create(StoreCreditRefund(returnId = returnId))
      sc ← * <~ StoreCredits.create(
              StoreCredit(accountId = accountId,
                          scope = Scope.current,
                          originId = origin.id,
                          originType = StoreCredit.RmaProcess,
                          state = StoreCredit.OnHold,
                          currency = payment.currency,
                          originalBalance = amount,
                          availableBalance = amount,
                          currentBalance = amount))
      pmt ← * <~ ReturnPayments.create(
               ReturnPayment(returnId = returnId,
                             amount = amount,
                             currency = payment.currency,
                             paymentMethodId = sc.id,
                             paymentMethodType = PaymentMethod.StoreCredit))
    } yield pmt

  def deletePayment(refNum: String, paymentMethod: PaymentMethod.Type)(
      implicit ec: EC,
      ac: AC,
      db: DB): DbResultT[ReturnResponse.Root] =
    for {
      rma               ← * <~ Returns.mustFindActiveByRefNum404(refNum)
      paymentWasDeleted ← * <~ processDeletePayment(rma.id, paymentMethod)
      updated           ← * <~ Returns.refresh(rma)
      response          ← * <~ ReturnResponse.fromRma(rma)

      _ ← * <~ doOrMeh(paymentWasDeleted,
                       LogActivity().returnPaymentsDeleted(response, List(paymentMethod)))
    } yield response

  private def deletePayments(rma: Return, payments: List[PaymentMethod.Type])(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[ReturnResponse.Root] =
    for {
      deleted ← * <~ payments.map(pmt ⇒
                     processDeletePayment(rma.id, pmt).product(DbResultT.pure(pmt)))
      deletedPayments = deleted.collect { case (true, pmt) ⇒ pmt }
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
      _ ← * <~ doOrMeh(deletedPayments.nonEmpty,
                       LogActivity().returnPaymentsDeleted(response, deletedPayments))
    } yield response

  private def processDeletePayment(returnId: Int, paymentMethod: PaymentMethod.Type)(
      implicit ec: EC): DbResultT[Boolean] =
    paymentMethod match {
      case PaymentMethod.CreditCard  ⇒ deleteCcPayment(returnId)
      case PaymentMethod.GiftCard    ⇒ deleteGcPayment(returnId)
      case PaymentMethod.StoreCredit ⇒ deleteScPayment(returnId)
      case PaymentMethod.ApplePay    ⇒ deleteApPayment(returnId)
    }

  private def deleteCcPayment(returnId: Int)(implicit ec: EC): DbResultT[Boolean] =
    ReturnPayments.findAllByReturnId(returnId).creditCards.deleteAllWithRowsBeingAffected

  private def deleteGcPayment(returnId: Int)(implicit ec: EC): DbResultT[Boolean] = {
    val gcQuery = ReturnPayments.findAllByReturnId(returnId).giftCards
    for {
      paymentMethodIds ← * <~ gcQuery.paymentMethodIds.result
      giftCardOriginIds ← * <~ GiftCards
                           .findAllByIds(paymentMethodIds)
                           .map(_.originId)
                           .to[Set]
                           .result
      queryDeleted ← * <~ gcQuery.deleteAllWithRowsBeingAffected
      gcDeleted    ← * <~ GiftCards.findAllByIds(paymentMethodIds).deleteAllWithRowsBeingAffected
      gcRefundsDeleted ← * <~ GiftCardRefunds
                          .findAllByIds(giftCardOriginIds)
                          .deleteAllWithRowsBeingAffected
      somethingWasActuallyDeleted = queryDeleted || gcDeleted || gcRefundsDeleted
    } yield somethingWasActuallyDeleted
  }

  private def deleteScPayment(returnId: Int)(implicit ec: EC): DbResultT[Boolean] = {
    val scQuery = ReturnPayments.findAllByReturnId(returnId).storeCredits
    for {
      paymentMethodIds ← * <~ scQuery.paymentMethodIds.result
      storeCreditOriginIds ← * <~ StoreCredits
                              .findAllByIds(paymentMethodIds)
                              .map(_.originId)
                              .to[Set]
                              .result
      queryDeleted ← * <~ scQuery.deleteAllWithRowsBeingAffected
      scDeleted    ← * <~ StoreCredits.findAllByIds(paymentMethodIds).deleteAllWithRowsBeingAffected
      scRefundsDeleted ← * <~ StoreCreditRefunds
                          .findAllByIds(storeCreditOriginIds)
                          .deleteAllWithRowsBeingAffected
      somethingWasActuallyDeleted = queryDeleted || scDeleted || scRefundsDeleted
    } yield somethingWasActuallyDeleted
  }

  def deleteApPayment(returnId: Int)(implicit ec: EC): DbResultT[Boolean] =
    DbResultT.pure(true) // TODO implement AP returns @aafa

  def issueRefunds(
      rma: Return)(implicit ec: EC, db: DB, au: AU, ac: AC, apis: Apis): DbResultT[Unit] = {
    for {
      customer ← * <~ Users.mustFindByAccountId(rma.accountId)

      // we are assuming that refund will go to the first external payment we have on records
      stripePayment ← * <~ ReturnPayments.findAllByReturnId(rma.id).externalPayments.one
      _             ← * <~ stripePayment.map(issueStripeRefund(rma, _))

      gc ← * <~ ReturnPayments.findOnHoldGiftCards(rma.id).one
      _  ← * <~ gc.map(issueGcRefund(customer, rma, _))

      sc ← * <~ ReturnPayments.findOnHoldStoreCredits(rma.id).one
      _  ← * <~ sc.map(issueScRefund(customer, rma, _))
    } yield ()
  }

  private def issueStripeRefund(
      rma: Return,
      payment: ReturnPayment)(implicit ec: EC, db: DB, ac: AC, apis: Apis): DbResultT[Unit] = {
    val authorizeRefund =
      ((id: String, amount: Int) ⇒
         for {
           _ ← * <~ apis.stripe.authorizeRefund(id, amount, RefundReason.RequestedByCustomer)
           stripePayment = ReturnStripePayment(
               returnPaymentId = payment.id,
               chargeId = id,
               returnId = payment.returnId,
               amount = amount,
               currency = payment.currency
           )
           created ← * <~ ReturnStripePayments.create(stripePayment)
         } yield created).tupled
        .andThen(_.runTxn()) // we want to run each stripe refund in separate transaction to avoid any rollback of `ReturnCcPayments` table

    /** Splits total refund amount into order cc charges.
      *
      * Each charge is used either up to its maximum or to the refund amount left (whichever is less).
      * Note that subsequent charges will be taken into account only if amount left to split will be greater than 0.
      */
    @tailrec
    def splitAmount(amount: Int,
                    charges: Seq[StripeOrderPayment],
                    acc: Vector[(String, Int)]): Vector[(String, Int)] = charges match {
      case c +: cs if amount > c.amount ⇒
        splitAmount(amount - c.amount, cs, acc :+ (c.stripeChargeId → c.amount))
      case c +: cs if amount <= c.amount ⇒
        acc :+ (c.stripeChargeId → amount)
      case _ ⇒ acc
    }

    def checkCurrency(orderPayments: Seq[StripeOrderPayment]) = {
      val mismatchedCharges = orderPayments.collect {
        case charge if charge.currency != payment.currency ⇒ charge.currency
      }
      failIf(mismatchedCharges.nonEmpty,
             ReturnStripePaymentCurrencyMismatch(refNum = rma.refNum,
                                                 expected = payment.currency,
                                                 actual = mismatchedCharges.toList))
    }

    for {
      previousStripeRefunds ← * <~ Returns
                               .findPreviousOrCurrent(rma)
                               .join(ReturnStripePayments)
                               .on(_.id === _.returnId)
                               .map { case (_, payment) ⇒ payment.chargeId → payment.amount }
                               .groupBy(_._1)
                               .map {
                                 case (id, payment) ⇒ id → payment.map(_._2).sum.getOrElse(0)
                               }
                               .result
                               .map(_.toMap)

      stripeOrderPayments ← * <~ OrderPayments.findAllStripeCharges(rma.orderRef).result
      _                   ← * <~ checkCurrency(stripeOrderPayments)

      adjustedStripeCharges = stripeOrderPayments.map { payment ⇒
        payment.copy(
            amount = payment.amount - previousStripeRefunds.getOrElse(payment.stripeChargeId, 0))
      }.filter(_.amount > 0)

      amountToRefund = splitAmount(payment.amount, adjustedStripeCharges, Vector.empty)

      refunds ← * <~ amountToRefund.map(authorizeRefund).sequenceU
      totalRefund = refunds.map(_.amount).sum

      _ ← * <~ failIf(totalRefund != payment.amount,
                      ReturnStripePaymentViolation(refNum = rma.refNum,
                                                   issued = totalRefund,
                                                   allowed = payment.amount))
      _ ← * <~ LogActivity().issueStripeRefund(rma, payment)
    } yield ()
  }

  private def issueGcRefund(customer: User, rma: Return, gc: GiftCard)(implicit ec: EC,
                                                                       ac: AC,
                                                                       au: AU): DbResultT[Unit] =
    for {
      gc ← * <~ GiftCards.update(gc,
                                 gc.copy(state = GiftCard.Active,
                                         senderName = au.model.name,
                                         recipientName = customer.name,
                                         recipientEmail = customer.email))
      _ ← * <~ LogActivity().issueGcRefund(customer, rma, gc)
    } yield ()

  private def issueScRefund(customer: User,
                            rma: Return,
                            sc: StoreCredit)(implicit ec: EC, ac: AC, au: AU): DbResultT[Unit] =
    for {
      sc ← * <~ StoreCredits.update(sc, sc.copy(state = StoreCredit.Active))
      _  ← * <~ LogActivity().issueScRefund(customer, rma, sc)
    } yield ()

  def cancelRefunds(rma: Return)(implicit ec: EC, ac: AC): DbResultT[Unit] =
    for {
      gc ← * <~ ReturnPayments.findOnHoldGiftCards(rma.id).one
      _ ← * <~ gc.map { gc ⇒
           GiftCards.update(gc,
                            gc.copy(state = GiftCard.Canceled,
                                    canceledAmount = gc.availableBalance.some,
                                    canceledReason = rma.canceledReasonId))
         }
      sc ← * <~ ReturnPayments.findOnHoldStoreCredits(rma.id).one
      _ ← * <~ sc.map { sc ⇒
           StoreCredits.update(sc,
                               sc.copy(state = StoreCredit.Canceled,
                                       canceledAmount = sc.availableBalance.some,
                                       canceledReason = rma.canceledReasonId))
         }
      _ ← * <~ LogActivity().cancelRefund(rma)
    } yield ()
}
