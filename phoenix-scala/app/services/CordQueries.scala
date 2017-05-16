package services

import models.cord.CordPaymentState._
import models.cord.{CordPaymentState, OrderPayment, OrderPayments}
import models.payment.PaymentMethod
import models.payment.creditcard.CreditCardCharges
import models.payment.giftcard.GiftCardAdjustments
import models.payment.storecredit.StoreCreditAdjustments
import slick.dbio.DBIO
import utils.aliases.EC
import utils.db.ExPostgresDriver.api._

trait CordQueries {

  def getCordPaymentState(cordRef: String)(implicit ec: EC): DBIO[State] =
    for {
      payments  ← OrderPayments.findAllByCordRef(cordRef).result
      payStates ← DBIO.sequence(payments.map(getPaymentState)).map(_.flatten)
    } yield CordQueries.foldPaymentStates(payStates, payments.size)

  private def getPaymentState(payment: OrderPayment)(implicit ec: EC): DBIO[Option[State]] = {
    payment.paymentMethodType match {
      case PaymentMethod.CreditCard ⇒
        CreditCardCharges
          .filter(_.orderPaymentId === payment.id)
          .map(_.state)
          .result
          .headOption
          .map(_.map(fromCCState))
      case PaymentMethod.GiftCard ⇒
        GiftCardAdjustments.lastPaymentState(payment.id).map(_.map(fromInStoreState))
      case PaymentMethod.StoreCredit ⇒
        StoreCreditAdjustments.lastPaymentState(payment.id).map(_.map(fromInStoreState))
    }
  }
}

object CordQueries {
  def foldPaymentStates(paymentStates: Seq[CordPaymentState.State],
                        rawPaymentsQuantity: ⇒ Int): CordPaymentState.State = {
    lazy val paymentsQuantity = rawPaymentsQuantity

    if (paymentStates
          .contains(Cart) || paymentsQuantity == 0 || paymentsQuantity != paymentStates.size) Cart
    else paymentStates.deduceCordPaymentState
  }
}
