package services.carts

import cats._
import cats.implicits._
import failures.NotFoundFailure404
import models.account._
import models.cord._
import models.objects.ObjectContext
import responses.TheResponse
import responses.cord.CartResponse
import services.{CartValidator, CordQueries, LineItemUpdater, LogActivity}
import utils.aliases._
import utils.db._

object CartQueries extends CordQueries {

  def findOne(refNum: String)(implicit ec: EC,
                              db: DB,
                              ctx: OC,
                              es: ES,
                              au: AU): DbResultT[TheResponse[CartResponse]] =
    for {
      cart ← * <~ Carts.mustFindByRefNum(refNum)
      resp ← * <~ LineItemUpdater.runUpdates(cart, None) // FIXME: so costly… @michalrus
    } yield resp

  def findOneByUser(refNum: String, customer: User, grouped: Boolean = true)(
      implicit ec: EC,
      db: DB,
      es: ES,
      au: AU,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart ← * <~ Carts
              .findByRefNumAndAccountId(refNum, customer.accountId)
              .mustFindOneOr(NotFoundFailure404(Carts, refNum))
      resp ← * <~ LineItemUpdater.runUpdates(cart, None) // FIXME: so costly… @michalrus
    } yield resp

  def findOrCreateCartByAccount(customer: User,
                                context: ObjectContext,
                                admin: Option[User] = None)(implicit ec: EC,
                                                            db: DB,
                                                            ac: AC,
                                                            ctx: OC,
                                                            es: ES,
                                                            au: AU): DbResultT[CartResponse] =
    findOrCreateCartByAccountInner(customer, admin)

  def findOrCreateCartByAccountId(accountId: Int,
                                  context: ObjectContext,
                                  admin: Option[User] = None)(implicit ec: EC,
                                                              db: DB,
                                                              ac: AC,
                                                              ctx: OC,
                                                              es: ES,
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
      es: ES,
      ctx: OC): DbResultT[CartResponse] =
    for {
      result ← * <~ Carts
                .findByAccountId(customer.accountId)
                .one
                .findOrCreateExtended(
                    Carts.create(Cart(accountId = customer.accountId, scope = Scope.current)))
      (cart, foundOrCreated) = result
      resp ← if (foundOrCreated == Created) for {
              fullCart ← * <~ CartResponse.fromCart(cart, grouped, au.isGuest)
              _        ← * <~ LogActivity().cartCreated(admin, fullCart)
            } yield TheResponse(fullCart)
            else LineItemUpdater.runUpdates(cart, None) // FIXME: so costly… @michalrus
    } yield
      resp.result // FIXME: discarding warnings until we get rid of TheResponse completely @michalrus
}
