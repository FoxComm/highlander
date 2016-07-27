package services.carts

import cats.implicits._
import models.StoreAdmin
import models.cord.{Cart, Carts}
import models.customer.{Customer, Customers}
import payloads.OrderPayloads.CreateCart
import responses.cord.CartResponse
import services._
import utils.aliases._
import utils.db._

object CartCreator {

  def createCart(
      admin: StoreAdmin,
      payload: CreateCart)(implicit db: DB, ec: EC, ac: AC, ctx: OC): DbResultT[CartResponse] = {

    def existingCustomerOrNewGuest: DbResultT[CartResponse] =
      (payload.customerId, payload.email) match {
        case (Some(customerId), _) ⇒ createCartForCustomer(customerId)
        case (_, Some(email))      ⇒ createCartAndGuest(email)
        case _                     ⇒ ???
      }

    def createCartForCustomer(customerId: Int)(implicit ctx: OC): DbResultT[CartResponse] =
      for {
        customer ← * <~ Customers.mustFindById400(customerId)
        fullCart ← * <~ CartQueries.findOrCreateCartByCustomerInner(customer, Some(admin))
      } yield fullCart

    def createCartAndGuest(email: String): DbResultT[CartResponse] =
      for {
        guest ← * <~ Customers.create(Customer.buildGuest(email = email.some))
        cart  ← * <~ Carts.create(Cart(customerId = guest.id))
        _     ← * <~ LogActivity.cartCreated(Some(admin), root(cart, guest))
      } yield root(cart, guest)

    for {
      _    ← * <~ payload.validate.toXor
      root ← * <~ existingCustomerOrNewGuest
    } yield root
  }

  private def root(cart: Cart, customer: Customer): CartResponse =
    CartResponse.buildEmpty(cart = cart, customer = customer.some)
}
