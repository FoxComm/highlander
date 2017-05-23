package phoenix.services

import phoenix.models.cord.CordPaymentState._
import phoenix.models.cord.{OrderPayment, OrderPayments}
import phoenix.models.payment.PaymentMethod
import phoenix.models.payment.creditcard.CreditCardCharges
import phoenix.models.payment.giftcard.GiftCardAdjustments
import phoenix.models.payment.storecredit.StoreCreditAdjustments
import phoenix.utils.aliases.EC
import slick.dbio.DBIO
import utils.db.ExPostgresDriver.api._

trait CordQueries {

  def getCordPaymentState(cordRef: String)(implicit ec: EC): DBIO[State] =
    for {
      payments  ← OrderPayments.findAllByCordRef(cordRef).result
      payStates ← DBIO.sequence(payments.map(getPaymentState)).map(_.flatten)
    } yield {
      if (payStates.contains(Cart) || payments.size != payStates.size || payments.isEmpty) Cart
      else payStates.deduceCordPaymentState
    }

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
