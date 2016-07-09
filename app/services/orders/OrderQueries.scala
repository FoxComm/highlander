package services.orders

import cats.implicits._
import models.cord._
import models.customer.{Customer, Customers}
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import responses.TheResponse
import responses.order.AllOrders
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object OrderQueries {

  def findAllByQuery(query: Orders.QuerySeq = Orders)(
      implicit ec: EC): DbResultT[TheResponse[Seq[AllOrders.Root]]] = {

    def build(order: Order, customer: Customer) =
      for {
        paymentState ← * <~ getPaymentState(order.refNum)
      } yield AllOrders.build(order, customer.some, paymentState.some)

    for {
      ordersCustomers ← * <~ query.join(Customers).on(_.customerId === _.id).result
      response        ← * <~ ordersCustomers.map((build _).tupled)
    } yield TheResponse.build(response)
  }

  // TODO dedup
  def getPaymentState(cordRef: String)(implicit ec: EC): DBIO[CreditCardCharge.State] =
    for {
      payments ← OrderPayments.findAllByOrderRef(cordRef).result
      authorized ← DBIO.sequence(payments.map(payment ⇒
                            payment.paymentMethodType match {
                      case PaymentMethod.CreditCard ⇒
                        import CreditCardCharge._
                        CreditCardCharges
                          .filter(_.orderPaymentId === payment.id)
                          .filter(_.state === (Auth: State))
                          .size
                          .result
                      case PaymentMethod.GiftCard ⇒
                        import GiftCardAdjustment._
                        GiftCardAdjustments
                          .filter(_.orderPaymentId === payment.id)
                          .filter(_.state === (Auth: State))
                          .size
                          .result
                      case PaymentMethod.StoreCredit ⇒
                        import StoreCreditAdjustment._
                        StoreCreditAdjustments
                          .filter(_.orderPaymentId === payment.id)
                          .filter(_.state === (Auth: State))
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
