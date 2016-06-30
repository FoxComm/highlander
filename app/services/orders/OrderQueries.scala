package services.orders

import cats.implicits._
import failures.NotFoundFailure404
import models.StoreAdmin
import models.customer.{Customer, Customers}
import models.objects.ObjectContext
import models.order._
import models.payment.PaymentMethod
import models.payment.creditcard._
import models.payment.giftcard._
import models.payment.storecredit._
import responses.TheResponse
import responses.order._
import services.{CartValidator, LogActivity, Result}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object OrderQueries {

  def findAllByQuery(query: Orders.QuerySeq = Orders)(
      implicit ec: EC): DbResultT[TheResponse[Seq[AllOrders.Root]]] = {

    def build(order: Order, customer: Customer) =
      for {
        paymentState ← * <~ getPaymentState(order.refNum).toXor
      } yield AllOrders.build(order, customer.some, paymentState.some)

    for {
      ordersCustomers ← * <~ query.join(Customers).on(_.customerId === _.id).result.toXor
      response        ← * <~ ordersCustomers.map((build _).tupled)
    } yield TheResponse.build(response)
  }

  def findOne(refNum: String)(implicit ec: EC, db: DB): Result[TheResponse[FullOrder.Root]] =
    (for {
      order     ← * <~ Orders.mustFindByRefNum(refNum)
      validated ← * <~ CartValidator(order).validate()
      response  ← * <~ FullOrder.fromOrder(order)
    } yield
      TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).run()

  def findOneByCustomer(refNum: String, customer: Customer)(
      implicit ec: EC,
      db: DB): Result[TheResponse[FullOrder.Root]] =
    (for {
      order ← * <~ Orders
               .findOneByRefNumAndCustomer(refNum, customer)
               .mustFindOneOr(NotFoundFailure404(Orders, refNum))
      validated ← * <~ CartValidator(order).validate()
      response  ← * <~ FullOrder.fromOrder(order)
    } yield
      TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)).run()

  def findOrCreateCartByCustomer(
      customer: Customer,
      context: ObjectContext,
      admin: Option[StoreAdmin] = None)(implicit ec: EC, db: DB, ac: AC): Result[FullOrder.Root] =
    findOrCreateCartByCustomerInner(customer, context, admin).runTxn()

  def findOrCreateCartByCustomerId(
      customerId: Int,
      context: ObjectContext,
      admin: Option[StoreAdmin] = None)(implicit ec: EC, db: DB, ac: AC): Result[FullOrder.Root] =
    (for {
      customer  ← * <~ Customers.mustFindById404(customerId)
      fullOrder ← * <~ findOrCreateCartByCustomerInner(customer, context, admin)
    } yield fullOrder).runTxn()

  def findOrCreateCartByCustomerInner(
      customer: Customer,
      context: ObjectContext,
      admin: Option[StoreAdmin])(implicit db: DB, ec: EC, ac: AC): DbResultT[FullOrder.Root] =
    for {
      result ← * <~ Orders
                .findActiveOrderByCustomer(customer)
                .one
                .findOrCreateExtended(Orders.create(Order.buildCart(customer.id, context.id)))
      (order, foundOrCreated) = result
      fullOrder ← * <~ FullOrder.fromOrder(order)
      _         ← * <~ logCartCreation(foundOrCreated, fullOrder, admin)
    } yield fullOrder

  private def logCartCreation(foundOrCreated: FoundOrCreated,
                              order: FullOrder.Root,
                              admin: Option[StoreAdmin])(implicit ec: EC, ac: AC) =
    foundOrCreated match {
      case Created ⇒ LogActivity.cartCreated(admin, order)
      case Found   ⇒ DbResultT.unit
    }

  def getPaymentState(orderRef: String)(implicit ec: EC): DBIO[CreditCardCharge.State] =
    for {
      payments ← OrderPayments.findAllByOrderRef(orderRef).result
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
