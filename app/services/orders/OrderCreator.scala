package services.orders

import scala.concurrent.ExecutionContext

import cats.data.Xor
import services.CartFailures._
import cats.implicits._
import models.Orders.scope._
import models.{Customer, Customers, Order, Orders}
import payloads.CreateOrder
import responses.FullOrder
import responses.FullOrder.Root
import services._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._

object OrderCreator {
  def createCart(payload: CreateOrder)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = {

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
      } yield root(newCart, customer)).runT()

    def createCartAndGuest(email: String): Result[Root] = (for {
      guest ← * <~ Customers.create(Customer.buildGuest(email = email))
      cart  ← * <~ Orders.create(Order.buildCart(guest.id))
    } yield root(cart, guest)).runT()

    (for {
      _     ← ResultT.fromXor(payload.validate.toXor)
      root  ← ResultT(existingCustomerOrNewGuest)
    } yield root).value
  }

  private def root(order: Order, customer: Customer): Root = FullOrder.build(order = order, customer = customer.some)
}

