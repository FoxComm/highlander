package services.carts

import failures.NotFoundFailure404
import models.account._
import models.cord._
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

  def findOneByAccount(
      refNum: String,
      customer: User)(implicit ec: EC, db: DB, ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart ← * <~ Carts
              .findByRefNumAndAccountId(refNum, customer.accountId)
              .mustFindOneOr(NotFoundFailure404(Carts, refNum))
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.fromCart(cart)
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)

  def findOrCreateCartByAccount(customer: User,
                                context: ObjectContext,
                                admin: Option[User] = None)(implicit ec: EC,
                                                            db: DB,
                                                            ac: AC,
                                                            ctx: OC): DbResultT[CartResponse] =
    findOrCreateCartByAccountInner(customer, admin)

  def findOrCreateCartByAccountId(accountId: Int,
                                  context: ObjectContext,
                                  admin: Option[User] = None)(implicit ec: EC,
                                                              db: DB,
                                                              ac: AC,
                                                              ctx: OC): DbResultT[CartResponse] =
    for {
      customer  ← * <~ Users.mustFindByAccountId(accountId)
      fullOrder ← * <~ findOrCreateCartByAccountInner(customer, admin)
    } yield fullOrder

  def findOrCreateCartByAccountInner(
      customer: User,
      admin: Option[User])(implicit db: DB, ec: EC, ac: AC, ctx: OC): DbResultT[CartResponse] =
    for {
      result ← * <~ Carts
                .findByAccountId(customer.accountId)
                .one
                .findOrCreateExtended(Carts.create(Cart(accountId = customer.accountId)))
      (cart, foundOrCreated) = result
      fullOrder ← * <~ CartResponse.fromCart(cart)
      _         ← * <~ logCartCreation(foundOrCreated, fullOrder, admin)
    } yield fullOrder

  private def logCartCreation(foundOrCreated: FoundOrCreated,
                              cart: CartResponse,
                              admin: Option[User])(implicit ec: EC, ac: AC) =
    foundOrCreated match {
      case Created ⇒ LogActivity.cartCreated(admin, cart)
      case Found   ⇒ DbResultT.unit
    }
}
