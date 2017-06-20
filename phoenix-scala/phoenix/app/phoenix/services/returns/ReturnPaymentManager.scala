package phoenix.services.returns

import cats.implicits._
import core.db._
import core.failures.GeneralFailure
import phoenix.failures.OrderFailures.{OnlyOneExternalPaymentIsAllowed, OrderPaymentNotFoundFailure}
import phoenix.failures.ReturnFailures._
import phoenix.models.account.{Scope, User, Users}
import phoenix.models.cord.OrderPayments.scope._
import phoenix.models.cord._
import phoenix.models.payment.PaymentMethod
import phoenix.models.payment.creditcard.{CreditCardCharge, CreditCardCharges, CreditCards}
import phoenix.models.payment.giftcard._
import phoenix.models.payment.storecredit._
import phoenix.models.returns.ReturnPayments.scope._
import phoenix.models.returns._
import phoenix.responses.ReturnResponse
import phoenix.services.LogActivity
import phoenix.services.carts.CartTotaler
import phoenix.utils.aliases._
import phoenix.utils.apis.{Apis, RefundReason}
import slick.jdbc.PostgresProfile.api._
import core.utils.Money._
import scala.annotation.tailrec
import core.db._
import phoenix.models.payment.applepay.ApplePayments

object ReturnPaymentManager {
  def updatePayments(
      refNum: String,
      payments: Map[PaymentMethod.Type, Long],
      overwrite: Boolean)(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[ReturnResponse.Root] = {

    @inline
    def addPayment(rma: Return, payment: OrderPayment, paymentMethodAmount: (PaymentMethod.Type, Long)) = {
      val (method, amount) = paymentMethodAmount
      processAddPayment(rma, payment, method, amount)
    }

    val paymentsToAdd         = payments.filter { case (_, amount) ⇒ amount > 0 }
    lazy val paymentsToRemove = (PaymentMethod.Type.types -- paymentsToAdd.keySet).toList

    if (paymentsToAdd.isEmpty)
      for {
        rma      ← * <~ Returns.mustFindActiveByRefNum404(refNum)
        response ← * <~ ifElse(overwrite, deletePayments(rma, paymentsToRemove), ReturnResponse.fromRma(rma))
      } yield response
    else
      for {
        _ ← * <~ failIf(paymentsToAdd.filter { case (pt, _) ⇒ pt.isExternal }.groupBy(_._1).size > 1,
                        OnlyOneExternalPaymentIsAllowed)
        rma     ← * <~ Returns.mustFindActiveByRefNum404(refNum)
        payment ← * <~ mustFindExternalPaymentsByOrderRef(rma.orderRef)
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

  private def validateMaxAllowedPayments(rma: Return,
                                         payments: Map[PaymentMethod.Type, Long],
                                         sumOther: Boolean)(implicit ec: EC, db: DB): DbResultT[Unit] = {
    def validateTotalPayment() =
      for {
        adjustments ← * <~ ReturnTotaler.adjustmentsTotal(rma)
        subTotal    ← * <~ ReturnTotaler.subTotal(rma)
        shipping ← * <~ ReturnLineItemShippingCosts
                    .findByRmaId(rma.id)
                    .map(_.amount)
                    .sum
                    .getOrElse(0L)
                    .result
        taxes ← * <~ CartTotaler.taxesTotal(cordRef = rma.orderRef,
                                            subTotal = subTotal,
                                            shipping = shipping,
                                            adjustments = adjustments)
        maxAmount = (subTotal + taxes + shipping - adjustments).zeroIfNegative
        amount    = payments.valuesIterator.sum

        _ ← * <~ failIf(
             amount > maxAmount,
             ReturnPaymentExceeded(refNum = rma.referenceNumber, amount = amount, maxAmount = maxAmount))
      } yield ()

    def validateCCPayment() = {
      val orderCCPaymentQuery = OrderPayments
        .findAllCreditCardsForOrder(rma.orderRef)
        .join(CreditCardCharges)
        .on(_.id === _.orderPaymentId)
        .map { case (_, charge) ⇒ charge.amount }
        .sum
        .getOrElse(0L)
        .result
      val previousCCPaymentsQuery = Returns
        .findPrevious(rma)
        .join(ReturnPayments.creditCards)
        .on(_.id === _.returnId)
        .map { case (_, payment) ⇒ payment.amount }
        .sum
        .getOrElse(0L)
        .result
      val ccAmount = payments.getOrElse(PaymentMethod.CreditCard, 0L)

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

  private def mustFindExternalPaymentsByOrderRef(cordRef: String)(implicit ec: EC): DbResultT[OrderPayment] =
    OrderPayments
      .findAllByCordRef(cordRef)
      .externalPayments
      .mustFindOneOr(OrderPaymentNotFoundFailure(Order))

  private def processAddPayment(rma: Return, payment: OrderPayment, method: PaymentMethod.Type, amount: Long)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[ReturnPayment] =
    method match {
      case PaymentMethod.CreditCard ⇒ addCreditCard(rma.id, payment, amount)
      case PaymentMethod.GiftCard   ⇒ addGiftCard(rma.id, payment, amount)
      case PaymentMethod.ApplePay   ⇒ addApplePayment(rma.id, payment, amount)
      case PaymentMethod.StoreCredit ⇒
        addStoreCredit(returnId = rma.id, accountId = rma.accountId, payment, amount)
    }

  private def addCreditCard(returnId: Int, payment: OrderPayment, amount: Long)(
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

  private def addApplePayment(returnId: Int, payment: OrderPayment, amount: Long)(implicit ec: EC,
                                                                                  db: DB,
                                                                                  au: AU) =
    for {
      ap ← * <~ ApplePayments.mustFindById404(payment.paymentMethodId)
      _  ← * <~ deleteApplePayPayment(returnId)
      applePayRefund ← * <~ ReturnPayments.create(
                        ReturnPayment(returnId = returnId,
                                      amount = amount,
                                      currency = payment.currency,
                                      paymentMethodId = ap.id,
                                      paymentMethodType = PaymentMethod.ApplePay))
    } yield applePayRefund

  private def addGiftCard(returnId: Int,
                          payment: OrderPayment,
                          amount: Long)(implicit ec: EC, db: DB, au: AU): DbResultT[ReturnPayment] =
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

  private def addStoreCredit(returnId: Int, accountId: Int, payment: OrderPayment, amount: Long)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[ReturnPayment] =
    for {
      _      ← * <~ deleteScPayment(returnId)
      origin ← * <~ StoreCreditRefunds.create(StoreCreditRefund(returnId = returnId))
      sc ← * <~ StoreCredits.create(
            StoreCredit(
              accountId = accountId,
              scope = Scope.current,
              originId = origin.id,
              originType = StoreCredit.RmaProcess,
              state = StoreCredit.OnHold,
              currency = payment.currency,
              originalBalance = amount,
              availableBalance = amount,
              currentBalance = amount
            ))
      pmt ← * <~ ReturnPayments.create(
             ReturnPayment(returnId = returnId,
                           amount = amount,
                           currency = payment.currency,
                           paymentMethodId = sc.id,
                           paymentMethodType = PaymentMethod.StoreCredit))
    } yield pmt

  def deletePayment(
      refNum: String,
      paymentMethod: PaymentMethod.Type)(implicit ec: EC, ac: AC, db: DB): DbResultT[ReturnResponse.Root] =
    for {
      rma               ← * <~ Returns.mustFindActiveByRefNum404(refNum)
      paymentWasDeleted ← * <~ processDeletePayment(rma.id, paymentMethod)
      updated           ← * <~ Returns.refresh(rma)
      response          ← * <~ ReturnResponse.fromRma(rma)

      _ ← * <~ doOrMeh(paymentWasDeleted, LogActivity().returnPaymentsDeleted(response, List(paymentMethod)))
    } yield response

  private def deletePayments(
      rma: Return,
      payments: List[PaymentMethod.Type])(implicit ec: EC, db: DB, ac: AC): DbResultT[ReturnResponse.Root] =
    for {
      deleted ← * <~ payments.map(pmt ⇒ processDeletePayment(rma.id, pmt).product(DbResultT.pure(pmt)))
      deletedPayments = deleted.collect { case (true, pmt) ⇒ pmt }
      updated  ← * <~ Returns.refresh(rma)
      response ← * <~ ReturnResponse.fromRma(updated)
      _ ← * <~ doOrMeh(deletedPayments.nonEmpty,
                       LogActivity().returnPaymentsDeleted(response, deletedPayments))
    } yield response

  private def processDeletePayment(returnId: Int, paymentMethod: PaymentMethod.Type)(
      implicit ec: EC): DbResultT[Boolean] =
    paymentMethod match {
      case PaymentMethod.GiftCard    ⇒ deleteGcPayment(returnId)
      case PaymentMethod.StoreCredit ⇒ deleteScPayment(returnId)
      case PaymentMethod.CreditCard  ⇒ deleteCcPayment(returnId)
      case PaymentMethod.ApplePay    ⇒ deleteApplePayPayment(returnId)
    }

  private def deleteCcPayment(returnId: Int)(implicit ec: EC): DbResultT[Boolean] =
    ReturnPayments.findAllByReturnId(returnId).creditCards.deleteAllWithRowsBeingAffected

  private def deleteApplePayPayment(returnId: Int)(implicit ec: EC): DbResultT[Boolean] =
    ReturnPayments.findAllByReturnId(returnId).applePays.deleteAllWithRowsBeingAffected

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

  def issueRefunds(rma: Return)(implicit ec: EC, db: DB, au: AU, ac: AC, apis: Apis): DbResultT[Unit] =
    for {
      customer ← * <~ Users.mustFindByAccountId(rma.accountId)

      ccPayment ← * <~ ReturnPayments.findAllByReturnId(rma.id).creditCards.one
      _         ← * <~ ccPayment.map(issueCcRefund(rma, _))

      gc ← * <~ ReturnPayments.findOnHoldGiftCards(rma.id).one
      _  ← * <~ gc.map(issueGcRefund(customer, rma, _))

      sc ← * <~ ReturnPayments.findOnHoldStoreCredits(rma.id).one
      _  ← * <~ sc.map(issueScRefund(customer, rma, _))
    } yield ()

  private def issueCcRefund(
      rma: Return,
      payment: ReturnPayment)(implicit ec: EC, db: DB, ac: AC, apis: Apis): DbResultT[Unit] = {
    val authorizeRefund =
      ((id: String,
        amount: Long) ⇒
         for {
           _ ← * <~ apis.stripe.authorizeRefund(id, amount, RefundReason.RequestedByCustomer)
           ccPayment = ReturnStripePayment(
             returnPaymentId = payment.id,
             chargeId = id,
             returnId = payment.returnId,
             amount = amount,
             currency = payment.currency
           )
           created ← * <~ ReturnStripePayments.create(ccPayment)
         } yield created).tupled
        .andThen(_.runTxn()) // we want to run each stripe refund in separate transaction to avoid any rollback of `ReturnStripePayments` table

    /** Splits total refund amount into order cc charges.
      *
      * Each charge is used either up to its maximum or to the refund amount left (whichever is less).
      * Note that subsequent charges will be taken into account only if amount left to split will be greater than 0.
      */
    @tailrec
    def splitAmount(amount: Long,
                    charges: Seq[CreditCardCharge],
                    acc: Vector[(String, Long)]): Vector[(String, Long)] = charges match {
      case c +: cs if amount > c.amount ⇒
        splitAmount(amount - c.amount, cs, acc :+ (c.stripeChargeId → c.amount))
      case c +: cs if amount <= c.amount ⇒
        acc :+ (c.stripeChargeId → amount)
      case _ ⇒ acc
    }

    def checkCurrency(ccCharges: Seq[CreditCardCharge]) = {
      val mismatchedCharges = ccCharges.collect {
        case charge if charge.currency != payment.currency ⇒ charge.currency
      }
      failIf(mismatchedCharges.nonEmpty,
             ReturnCcPaymentCurrencyMismatch(refNum = rma.refNum,
                                             expected = payment.currency,
                                             actual = mismatchedCharges.toList))
    }

    for {
      previousCcRefunds ← * <~ Returns
                           .findPreviousOrCurrent(rma)
                           .join(ReturnStripePayments)
                           .on(_.id === _.returnId)
                           .map { case (_, ccPayment) ⇒ ccPayment.chargeId → ccPayment.amount }
                           .groupBy(_._1)
                           .map {
                             case (id, ccPayment) ⇒
                               id → ccPayment.map(_._2).sum.getOrElse(0L)
                           }
                           .result
                           .map(_.toMap)
      ccCharges ← * <~ OrderPayments
                   .findAllCreditCardsForOrder(rma.orderRef)
                   .join(CreditCardCharges)
                   .on(_.id === _.orderPaymentId)
                   .map { case (_, charge) ⇒ charge }
                   .result
      _ ← * <~ checkCurrency(ccCharges)
      adjustedCcCharges = ccCharges
        .map(c ⇒ c.copy(amount = c.amount - previousCcRefunds.getOrElse(c.stripeChargeId, 0L)))
        .filter(_.amount > 0L)
      amountToRefund = splitAmount(payment.amount, adjustedCcCharges, Vector.empty)
      refunds ← * <~ amountToRefund.map(authorizeRefund).sequenceU
      totalRefund = refunds.map(_.amount).sum
      _ ← * <~ failIf(
           totalRefund != payment.amount,
           ReturnCcPaymentViolation(refNum = rma.refNum, issued = totalRefund, allowed = payment.amount))
      _ ← * <~ LogActivity().issueCcRefund(rma, payment)
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

  private def issueScRefund(customer: User, rma: Return, sc: StoreCredit)(implicit ec: EC,
                                                                          ac: AC,
                                                                          au: AU): DbResultT[Unit] =
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
