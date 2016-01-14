package services.orders

import cats.data.Xor
import cats.implicits._
import models.Orders.scope._
import models.{Customer, Customers, Order, Orders, StoreAdmin}
import payloads.CreateOrder
import responses.FullOrder
import responses.FullOrder.Root
import services.CartFailures._
import services.{LogActivity, NotFoundFailure400, Result, ResultT}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._

import scala.concurrent.ExecutionContext
import models.activity.ActivityContext

object OrderCreator {
  def createCart(admin: StoreAdmin, payload: CreateOrder)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[Root] = {

    def existingCustomerOrNewGuest: Result[Root] = (payload.customerId, payload.email) match {
      case (Some(customerId), _)  ⇒ createCartForCustomer(customerId)
      case (_, Some(email))       ⇒ createCartAndGuest(email)
      case _                      ⇒ ???
    }

    def createCartForCustomer(customerId: Int): Result[Root] = (for {
        customer ← * <~ Customers.mustFindById(customerId, i ⇒ NotFoundFailure400(Customer, i))
        hasCart  ← * <~ Orders.findByCustomerId(customerId).cartOnly.exists.result
        _        ← * <~ (if (hasCart) Xor.left(CustomerHasCart(customer.id).single) else Xor.right({}))
        newCart  ← * <~ Orders.create(Order.buildCart(customerId))
        _        ← * <~ LogActivity.cartCreated(admin, root(newCart, customer))
      } yield root(newCart, customer)).runTxn()

    def createCartAndGuest(email: String): Result[Root] = (for {
      guest ← * <~ Customers.create(Customer.buildGuest(email = email))
      cart  ← * <~ Orders.create(Order.buildCart(guest.id))
      _     ← * <~ LogActivity.cartCreated(admin, root(cart, guest))
    } yield root(cart, guest)).runTxn()

    (for {
      _     ← ResultT.fromXor(payload.validate.toXor)
      root  ← ResultT(existingCustomerOrNewGuest)
    } yield root).value
  }

  private def root(order: Order, customer: Customer): Root = FullOrder.build(order = order, customer = customer.some)
}

