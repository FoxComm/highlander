package services.carts

import failures.NotFoundFailure404
import models.StoreAdmin
import models.cord._
import models.customer.{Customer, Customers}
import models.objects.ObjectContext
import models.payment.creditcard.CreditCardCharge.{Auth ⇒ ccAuth}
import models.payment.creditcard._
import models.payment.giftcard.GiftCardAdjustment.{Auth ⇒ gcAuth}
import models.payment.storecredit.StoreCreditAdjustment.{Auth ⇒ scAuth}
import responses.TheResponse
import responses.cord.CartResponse
import services.{CartValidator, CordQueries, LogActivity}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CartQueries extends CordQueries {

  def findOne(
      refNum: String)(implicit ec: EC, db: DB, ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart      ← * <~ Carts.mustFindByRefNum(refNum)
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.fromCart(cart)
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)

  def findOneByCustomer(
      refNum: String,
      customer: Customer)(implicit ec: EC, db: DB, ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart ← * <~ Carts
              .findByRefNumAndCustomer(refNum, customer)
              .mustFindOneOr(NotFoundFailure404(Carts, refNum))
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.fromCart(cart)
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)

  def findOrCreateCartByCustomer(customer: Customer,
                                 context: ObjectContext,
                                 admin: Option[StoreAdmin] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[CartResponse] =
    findOrCreateCartByCustomerInner(customer, admin)

  def findOrCreateCartByCustomerId(customerId: Int,
                                   context: ObjectContext,
                                   admin: Option[StoreAdmin] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[CartResponse] =
    for {
      customer  ← * <~ Customers.mustFindById404(customerId)
      fullOrder ← * <~ findOrCreateCartByCustomerInner(customer, admin)
    } yield fullOrder

  def findOrCreateCartByCustomerInner(customer: Customer, admin: Option[StoreAdmin])(
      implicit db: DB,
      ec: EC,
      ac: AC,
      ctx: OC): DbResultT[CartResponse] =
    for {
      result ← * <~ Carts
                .findByCustomer(customer)
                .one
                .findOrCreateExtended(Carts.create(Cart(customerId = customer.id)))
      (cart, foundOrCreated) = result
      fullOrder ← * <~ CartResponse.fromCart(cart)
      _         ← * <~ logCartCreation(foundOrCreated, fullOrder, admin)
    } yield fullOrder

  private def logCartCreation(foundOrCreated: FoundOrCreated,
                              cart: CartResponse,
                              admin: Option[StoreAdmin])(implicit ec: EC, ac: AC) =
    foundOrCreated match {
      case Created ⇒ LogActivity.cartCreated(admin, cart)
      case Found   ⇒ DbResultT.unit
    }
}
