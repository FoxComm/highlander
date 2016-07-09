package models

import scala.concurrent.ExecutionContext.Implicits.global

import failures.CartFailures.OrderAlreadyPlaced
import models.cord.Carts
import models.customer.Customers
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories

class CartsIntegrationTest extends IntegrationTestBase {

  "Carts" - {
    "generates a referenceNumber in Postgres after insert when blank" in new Fixture {
      val order =
        Carts.create(Factories.cart.copy(customerId = customer.id, referenceNumber = "")).gimme

      order.referenceNumber must === ("BR10001")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new Fixture {
      val order = Carts
        .create(Factories.cart.copy(customerId = customer.id, referenceNumber = "R123456"))
        .gimme
      order.referenceNumber must === ("R123456")
    }

    "can only have one cart per customer" in new Fixture {
      val cart = Carts.create(Factories.cart.copy(customerId = customer.id)).gimme

      val failure = Carts
        .create(cart.copy(id = 0, referenceNumber = cart.refNum + "ZZZ"))
        .run()
        .futureValue
        .leftVal
      failure.getMessage must include(
          """value violates unique constraint "customer_has_only_one_cart"""")
    }

    "has a unique index on referenceNumber" in new Fixture {
      val cart = Carts.create(Factories.cart.copy(customerId = customer.id)).gimme

      val failure = Carts.create(cart.copy(id = 0).copy(subTotal = 123)).run().futureValue.leftVal
      failure.getMessage must include(
          """value violates unique constraint "carts_reference_number_key"""")
    }

    "refuses to update inactive cart" in {
      val failure = Carts.create(Factories.cart.copy(isActive = false)).run().futureValue.leftVal
      failure must === (OrderAlreadyPlaced(Factories.cart.refNum).single)
    }
  }

  trait Fixture {
    val customer = Customers.create(Factories.customer).gimme
  }
}
