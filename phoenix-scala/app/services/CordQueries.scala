package services

import models.cord.{OrderPayment, OrderPayments}
import models.payment.PaymentStates
import models.payment.PaymentMethod
import models.payment.creditcard.CreditCardCharge._
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import slick.dbio.DBIO
import utils.aliases._
import utils.db.ExPostgresDriver.api._

trait CordQueries {

  // Using CreditCardCharge here as it has both Cart and Auth states. Consider refactoring.
  def getCordPaymentState(cordRef: String)(implicit ec: EC): DBIO[State] =
    for {
      payments ← OrderPayments.findAllByCordRef(cordRef).result
      charges  ← DBIO.sequence(payments.map(getPaymentState)).map(_.flatten)
    } yield {
      if (payments.size != charges.size || payments.isEmpty) Cart
      else if (charges.contains(ExpiredAuth)) ExpiredAuth
      else if (charges.contains(FailedCapture)) FailedCapture
      else if (charges.forall(_ == FullCapture)) FullCapture
      else Auth
    }

  private def getPaymentState(payment: OrderPayment)(implicit ec: EC): DBIO[Option[State]] = {
    def internalToCCState(state: Option[PaymentStates.State]) = state.map {
      case PaymentStates.Auth    ⇒ Auth
      case PaymentStates.Capture ⇒ FullCapture
      case _                     ⇒ Cart
    }

    payment.paymentMethodType match {
      case PaymentMethod.CreditCard ⇒
        CreditCardCharges.filter(_.orderPaymentId === payment.id).map(_.state).result.headOption
      case PaymentMethod.GiftCard ⇒
        GiftCardAdjustments.lastPaymentState(payment.id).map(internalToCCState)
      case PaymentMethod.StoreCredit ⇒
        StoreCreditAdjustments.lastPaymentState(payment.id).map(internalToCCState)
    }
  }
}
