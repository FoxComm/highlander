package services.carts

import cats.implicits._
import models.StoreAdmin
import models.cord.{Cart, Carts}
import models.customer.{Customer, Customers}
import payloads.OrderPayloads.CreateCart
import responses.cart.FullCart
import responses.cart.FullCart.Root
import services._
import utils.aliases._
import utils.db._

object CartCreator {

  def createCart(
      admin: StoreAdmin,
      payload: CreateCart)(implicit db: DB, ec: EC, ac: AC, ctx: OC): DbResultT[Root] = {

    def existingCustomerOrNewGuest: DbResultT[Root] = (payload.customerId, payload.email) match {
      case (Some(customerId), _) ⇒ createCartForCustomer(customerId)
      case (_, Some(email))      ⇒ createCartAndGuest(email)
      case _                     ⇒ ???
    }

    def createCartForCustomer(customerId: Int)(implicit ctx: OC): DbResultT[Root] =
      for {
        customer ← * <~ Customers.mustFindById400(customerId)
        fullCart ← * <~ CartQueries.findOrCreateCartByCustomerInner(customer, Some(admin))
      } yield fullCart

    def createCartAndGuest(email: String): DbResultT[Root] =
      for {
        guest ← * <~ Customers.create(Customer.buildGuest(email = email))
        cart  ← * <~ Carts.create(Cart(customerId = guest.id))
        _     ← * <~ LogActivity.cartCreated(Some(admin), root(cart, guest))
      } yield root(cart, guest)

    for {
      _    ← * <~ payload.validate.toXor
      root ← * <~ existingCustomerOrNewGuest
    } yield root
  }

  private def root(cart: Cart, customer: Customer): Root =
    FullCart.build(cart = cart, customer = customer.some)
}
