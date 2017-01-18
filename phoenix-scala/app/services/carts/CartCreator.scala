package services.carts

import cats.implicits._
import models.account._
import models.customer._
import models.cord.{Cart, Carts}
import payloads.OrderPayloads.CreateCart
import responses.cord.CartResponse
import services._
import utils.aliases._
import utils.db._

object CartCreator {

  def createCart(admin: User, payload: CreateCart)(implicit db: DB,
                                                   ec: EC,
                                                   ac: AC,
                                                   ctx: OC,
                                                   au: AU): DbResultT[CartResponse] = {

    def existingCustomerOrNewGuest: DbResultT[CartResponse] =
      (payload.customerId, payload.email) match {
        case (Some(customerId), _) ⇒ createCartForCustomer(customerId)
        case (_, Some(email))      ⇒ createCartAndGuest(email)
        case _                     ⇒ ???
      }

    def createCartForCustomer(accountId: Int)(implicit ctx: OC): DbResultT[CartResponse] =
      for {
        customer ← * <~ Users.mustFindByAccountId(accountId)
        fullCart ← * <~ CartQueries.findOrCreateCartByAccountInner(customer, Some(admin))
      } yield fullCart

    def createCartAndGuest(email: String): DbResultT[CartResponse] =
      for {
        account ← * <~ Accounts.create(Account())
        guest   ← * <~ Users.create(User(accountId = account.id, email = email.some))
        custData ← * <~ CustomersData.create(
          CustomerData(userId = guest.id,
                       accountId = account.id,
                       isGuest = true,
                       scope = Scope.current))
        scope ← * <~ Scope.resolveOverride(payload.scope)
        cart  ← * <~ Carts.create(Cart(accountId = account.id, scope = scope))
        _     ← * <~ LogActivity.cartCreated(Some(admin), root(cart, guest, custData))
      } yield root(cart, guest, custData)

    for {
      _    ← * <~ payload.validate.toXor
      root ← * <~ existingCustomerOrNewGuest
    } yield root
  }

  private def root(cart: Cart, customer: User, custData: CustomerData): CartResponse =
    CartResponse.buildEmpty(cart = cart, customer = customer.some, custData.some)
}
