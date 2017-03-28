package services.carts

import failures.NotFoundFailure404
import models.account._
import models.cord._
import models.objects.ObjectContext
import responses.TheResponse
import responses.cord.CartResponse
import services.{CartValidator, CordQueries, LogActivity}
import utils.aliases._
import utils.db._

object CartQueries extends CordQueries {

  def findOne(refNum: String, grouped: Boolean = true)(
      implicit ec: EC,
      db: DB,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart      ← * <~ Carts.mustFindByRefNum(refNum)
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.fromCart(cart, grouped)
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)

  def findOneByUser(refNum: String, customer: User, grouped: Boolean = true)(
      implicit ec: EC,
      db: DB,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart ← * <~ Carts
              .findByRefNumAndAccountId(refNum, customer.accountId)
              .mustFindOneOr(NotFoundFailure404(Carts, refNum))
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.fromCart(cart, grouped)
    } yield TheResponse.build(response, alerts = validated.alerts, warnings = validated.warnings)

  def findOrCreateCartByAccount(customer: User,
                                context: ObjectContext,
                                admin: Option[User] = None)(implicit ec: EC,
                                                            db: DB,
                                                            ac: AC,
                                                            ctx: OC,
                                                            au: AU): DbResultT[CartResponse] =
    findOrCreateCartByAccountInner(customer, admin)

  def findOrCreateCartByAccountId(accountId: Int,
                                  context: ObjectContext,
                                  admin: Option[User] = None)(implicit ec: EC,
                                                              db: DB,
                                                              ac: AC,
                                                              ctx: OC,
                                                              au: AU): DbResultT[CartResponse] =
    for {
      customer  ← * <~ Users.mustFindByAccountId(accountId)
      fullOrder ← * <~ findOrCreateCartByAccountInner(customer, admin)
    } yield fullOrder

  def findOrCreateCartByAccountInner(customer: User, admin: Option[User], grouped: Boolean = true)(
      implicit db: DB,
      ec: EC,
      ac: AC,
      au: AU,
      ctx: OC): DbResultT[CartResponse] =
    for {
      result ← * <~ Carts
                .findByAccountId(customer.accountId)
                .one
                .findOrCreateExtended(
                    Carts.create(Cart(accountId = customer.accountId, scope = Scope.current)))
      (cart, foundOrCreated) = result
      fullCart ← * <~ CartResponse.fromCart(cart, grouped, au.isGuest)
      _        ← * <~ doOrMeh(foundOrCreated == Created, LogActivity().cartCreated(admin, fullCart))
    } yield fullCart
}
