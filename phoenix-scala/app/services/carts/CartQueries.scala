package services.carts

import failures.NotFoundFailure404
import models.StoreAdmin
import models.cord._
import models.customer.{Customer, Customers}
import models.objects.ObjectContext
import models.payment.PaymentMethod
import models.payment.creditcard.CreditCardCharge.{Auth ⇒ ccAuth}
import models.payment.creditcard._
import models.payment.giftcard.GiftCardAdjustment.{Auth ⇒ gcAuth}
import models.payment.giftcard._
import models.payment.storecredit.StoreCreditAdjustment.{Auth ⇒ scAuth}
import models.payment.storecredit._
import responses.TheResponse
import responses.cart.FullCart
import services.{CartValidator, LogActivity}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CartQueries {

  def findOne(
      refNum: String)(implicit ec: EC, db: DB, ctx: OC): DbResultT[TheResponse[FullCart.Root]] =
    for {
      cart      ← * <~ Carts.mustFindByRefNum(refNum)
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ FullCart.fromCart(cart)
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)

  def findOneByCustomer(refNum: String, customer: Customer)(
      implicit ec: EC,
      db: DB,
      ctx: OC): DbResultT[TheResponse[FullCart.Root]] =
    for {
      cart ← * <~ Carts
              .findByRefNumAndCustomer(refNum, customer)
              .mustFindOneOr(NotFoundFailure404(Carts, refNum))
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ FullCart.fromCart(cart)
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)

  def findOrCreateCartByCustomer(customer: Customer,
                                 context: ObjectContext,
                                 admin: Option[StoreAdmin] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[FullCart.Root] =
    findOrCreateCartByCustomerInner(customer, admin)

  def findOrCreateCartByCustomerId(customerId: Int,
                                   context: ObjectContext,
                                   admin: Option[StoreAdmin] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[FullCart.Root] =
    for {
      customer  ← * <~ Customers.mustFindById404(customerId)
      fullOrder ← * <~ findOrCreateCartByCustomerInner(customer, admin)
    } yield fullOrder

  def findOrCreateCartByCustomerInner(customer: Customer, admin: Option[StoreAdmin])(
      implicit db: DB,
      ec: EC,
      ac: AC,
      ctx: OC): DbResultT[FullCart.Root] =
    for {
      result ← * <~ Carts
                .findByCustomer(customer)
                .one
                .findOrCreateExtended(Carts.create(Cart(customerId = customer.id)))
      (cart, foundOrCreated) = result
      fullOrder ← * <~ FullCart.fromCart(cart)
      _         ← * <~ logCartCreation(foundOrCreated, fullOrder, admin)
    } yield fullOrder

  private def logCartCreation(foundOrCreated: FoundOrCreated,
                              cart: FullCart.Root,
                              admin: Option[StoreAdmin])(implicit ec: EC, ac: AC) =
    foundOrCreated match {
      case Created ⇒ LogActivity.cartCreated(admin, cart)
      case Found   ⇒ DbResultT.unit
    }

  def getPaymentState(cordRef: String)(implicit ec: EC): DBIO[CreditCardCharge.State] =
    for {
      payments ← OrderPayments.findAllByOrderRef(cordRef).result
      authorized ← DBIO.sequence(payments.map(payment ⇒
                            payment.paymentMethodType match {
                      case PaymentMethod.CreditCard ⇒
                        CreditCardCharges
                          .filter(_.orderPaymentId === payment.id)
                          .filter(_.state === (ccAuth: CreditCardCharge.State))
                          .size
                          .result
                      case PaymentMethod.GiftCard ⇒
                        GiftCardAdjustments
                          .filter(_.orderPaymentId === payment.id)
                          .filter(_.state === (gcAuth: GiftCardAdjustment.State))
                          .size
                          .result
                      case PaymentMethod.StoreCredit ⇒
                        StoreCreditAdjustments
                          .filter(_.orderPaymentId === payment.id)
                          .filter(_.state === (scAuth: StoreCreditAdjustment.State))
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
