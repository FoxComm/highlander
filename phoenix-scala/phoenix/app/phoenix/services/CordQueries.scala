package phoenix.services

import core.db.ExPostgresDriver.api._
import phoenix.models.cord.CordPaymentState._
import phoenix.models.cord.{CordPaymentState, OrderPayment, OrderPayments}
import phoenix.models.payment.PaymentMethod
import phoenix.models.payment.creditcard.CreditCardCharges
import phoenix.models.payment.giftcard.GiftCardAdjustments
import phoenix.models.payment.storecredit.StoreCreditAdjustments
import phoenix.utils.aliases.EC
import phoenix.models.payment.applepay.ApplePayCharges
import slick.dbio.DBIO
import core.db._

trait CordQueries {

  def getCordPaymentState(cordRef: String)(implicit ec: EC): DBIO[State] =
    for {
      payments  ← OrderPayments.findAllByCordRef(cordRef).result
      payStates ← DBIO.sequence(payments.map(getPaymentState)).map(_.flatten)
    } yield CordQueries.foldPaymentStates(payStates, payments.size)

  private def getPaymentState(payment: OrderPayment)(implicit ec: EC): DBIO[Option[State]] =
    payment.paymentMethodType match {
      case PaymentMethod.CreditCard ⇒
        CreditCardCharges
          .filter(_.orderPaymentId === payment.id)
          .map(_.state)
          .one
          .map(_.map(fromExternalState))
      case PaymentMethod.GiftCard ⇒
        GiftCardAdjustments.lastPaymentState(payment.id).map(_.map(fromInStoreState))
      case PaymentMethod.StoreCredit ⇒
        StoreCreditAdjustments.lastPaymentState(payment.id).map(_.map(fromInStoreState))
      case PaymentMethod.ApplePay ⇒
        ApplePayCharges
          .filter(_.orderPaymentId === payment.id)
          .map(_.state)
          .one
          .map(_.map(fromExternalState))
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
