package phoenix.services.carts

import core.db._
import core.failures.NotFoundFailure404
import objectframework.models.ObjectContext
import phoenix.models.account._
import phoenix.models.cord._
import phoenix.responses.TheResponse
import phoenix.responses.cord.CartResponse
import phoenix.services.{CordQueries, LogActivity}
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

object CartQueries extends CordQueries {

  def findOne(refNum: String)(implicit ec: EC,
                              db: DB,
                              ctx: OC,
                              apis: Apis,
                              au: AU): DbResultT[TheResponse[CartResponse]] =
    for {
      cart ← * <~ Carts.mustFindByRefNum(refNum)
      resp ← * <~ CartLineItemUpdater.runUpdates(cart, None) // FIXME: so costly… @michalrus
    } yield resp

  def findOneByUser(refNum: String, customer: User, grouped: Boolean = true)(
      implicit ec: EC,
      db: DB,
      apis: Apis,
      au: AU,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      cart ← * <~ Carts
              .findByRefNumAndAccountId(refNum, customer.accountId)
              .mustFindOneOr(NotFoundFailure404(Carts, refNum))
      resp ← * <~ CartLineItemUpdater.runUpdates(cart, None) // FIXME: so costly… @michalrus
    } yield resp

  def findOrCreateCartByAccount(customer: User, context: ObjectContext, admin: Option[User] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC,
      apis: Apis,
      au: AU): DbResultT[CartResponse] =
    findOrCreateCartByAccountInner(customer, admin)

  def findOrCreateCartByAccountId(accountId: Int, context: ObjectContext, admin: Option[User] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      ctx: OC,
      apis: Apis,
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
      apis: Apis,
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
            else CartLineItemUpdater.runUpdates(cart, None) // FIXME: so costly… @michalrus
    } yield resp.result // FIXME: discarding warnings until we get rid of TheResponse completely @michalrus
}
