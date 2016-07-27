package services

import models.cord.OrderPayments
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._
import utils.aliases._

trait CordQueries {

  def getPaymentState(cordRef: String)(implicit ec: EC): DBIO[CreditCardCharge.State] =
    for {
      payments ← OrderPayments.findAllByCordRef(cordRef).result
      authorized ← DBIO.sequence(payments.map(payment ⇒
                            payment.paymentMethodType match {
                      case PaymentMethod.CreditCard ⇒
                        CreditCardCharges
                          .filter(_.orderPaymentId === payment.id)
                          .filter(_.state === (CreditCardCharge.Auth: CreditCardCharge.State))
                          .size
                          .result
                      case PaymentMethod.GiftCard ⇒
                        GiftCardAdjustments
                          .filter(_.orderPaymentId === payment.id)
                          .filter(_.state === (GiftCardAdjustment.Auth: GiftCardAdjustment.State))
                          .size
                          .result
                      case PaymentMethod.StoreCredit ⇒
                        import models.payment.storecredit.StoreCreditAdjustment._
                        StoreCreditAdjustments
                          .filter(_.orderPaymentId === payment.id)
                          .filter(
                              _.state === (StoreCreditAdjustment.Auth: StoreCreditAdjustment.State))
                          .size
                          .result
                  }))
      // Using CreditCardCharge here as it has both Cart and Auth states. Consider refactoring.
    } yield
      (payments.size, authorized.sum) match {
        case (0, _)                     ⇒ CreditCardCharge.Cart
        case (pmt, auth) if pmt == auth ⇒ CreditCardCharge.Auth
        case _                          ⇒ CreditCardCharge.Cart
      }

}
