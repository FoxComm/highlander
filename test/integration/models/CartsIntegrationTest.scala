package models

import models.cord.{Carts, Orders}
import util.Fixtures.CustomerFixture
import util.{IntegrationTestBase, TestObjectContext}
import utils.db._
import utils.seeds.Seeds.Factories

class CartsIntegrationTest extends IntegrationTestBase with TestObjectContext {

  "Carts" - {
    "generates a referenceNumber via a cord" in new CustomerFixture {
      val dummy = Factories.cart.copy(customerId = customer.id, referenceNumber = "")
      val cart1 = Carts.create(dummy).gimme
      cart1.referenceNumber must === ("BR10001")
      Orders.create(cart1.toOrder()).gimme
      val cart2 = Carts.create(dummy).gimme
      cart2.referenceNumber must === ("BR10002")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new CustomerFixture {
      val cart = Carts
        .create(Factories.cart.copy(customerId = customer.id, referenceNumber = "R123456"))
        .gimme
      cart.referenceNumber must === ("R123456")
    }

    "can only have one cart per customer" in new CustomerFixture {
      val cart = Carts.create(Factories.cart.copy(customerId = customer.id)).gimme

      val failure = Carts
        .create(cart.copy(id = 0, referenceNumber = cart.refNum + "ZZZ"))
        .run()
        .futureValue
        .leftVal
      failure.getMessage must include(
          """value violates unique constraint "customer_has_only_one_cart"""")
    }

    "has a unique index on referenceNumber" in new CustomerFixture {
      val cart = Carts.create(Factories.cart.copy(customerId = customer.id)).gimme

      val failure = Carts.create(cart.copy(id = 0).copy(subTotal = 123)).run().futureValue.leftVal
      failure.getMessage must include(
          """value violates unique constraint "cords_reference_number_key"""")
    }
  }
}
