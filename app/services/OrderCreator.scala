package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Xor
import cats.implicits._
import models.{Orders, Order, Customers, Customer}
import payloads.CreateOrder
import responses.FullOrder, FullOrder.Root
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import Orders.scope._

object OrderCreator {
  def createCart(payload: CreateOrder)
    (implicit db: Database, ec: ExecutionContext): Result[Root] = {

    (for {
      customer  ← Customers._findById(payload.customerId).extract.one
      hasCart   ← Orders.findByCustomerId(payload.customerId).cartOnly.exists.result
    } yield (customer, hasCart)).run().flatMap {
      case (Some(customer), false) ⇒
        Result.fromFuture(Orders.save(Order.buildCart(customer.id)).run().map { order ⇒
          FullOrder.build(order = order, customer = customer.some)
        })
      case (Some(customer), true) ⇒
        Result.failure(CustomerHasCart(customer.id))
      case _ ⇒
        Result.failure(NotFoundFailure(Customer, payload.customerId))
    }
  }
}

