package services.orders

import scala.concurrent.ExecutionContext

import cats.implicits._
import models.Orders.scope._
import models.{Customer, Customers, Order, Orders}
import payloads.CreateOrder
import responses.FullOrder
import responses.FullOrder.Root
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._

object OrderCreator {
  def createCart(payload: CreateOrder)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = {

    def existingCustomerOrNewGuest: Result[Root] = (payload.customerId, payload.email) match {
      case (Some(customerId), _)  ⇒ createCartForCustomer(customerId)
      case (_, Some(email))       ⇒ createCartAndGuest(email)
      case _                      ⇒ ???
    }

    def createCartForCustomer(customerId: Int): Result[Root] = (for {
      customer  ← Customers.findById(customerId).extract.one
      hasCart   ← Orders.findByCustomerId(customerId).cartOnly.exists.result
    } yield (customer, hasCart)).run().flatMap {
      case (Some(customer), false) ⇒
        Result.fromFuture(Orders.save(Order.buildCart(customerId)).run().map(root(_, customer)))

      case (Some(_), true) ⇒
        Result.failure(CustomerHasCart(customerId))

      case _ ⇒
        Result.failure(NotFoundFailure400(Customer, customerId))
    }

    def createCartAndGuest(email: String): Result[Root] = (for {
      guest ← Customers.save(Customer.buildGuest(email = email))
      cart  ← Orders.save(Order.buildCart(guest.id))
    } yield (cart, guest)).run().flatMap { case (cart, guest) ⇒
      Result.good(root(cart, guest))
    }

    (for {
      _     ← ResultT.fromXor(payload.validate.toXor)
      root  ← ResultT(existingCustomerOrNewGuest)
    } yield root).value
  }

  private def root(order: Order, customer: Customer): Root = FullOrder.build(order = order, customer = customer.some)
}

