package models

import models.cord.{Cart, Carts}
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CartsIntegrationTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "Carts" - {
    "generates a referenceNumber via a cord" in new Order_Baked {
      cart.referenceNumber must === ("BR10001")

      val cart2 = Carts.create(cart.copy(id = 0, referenceNumber = "")).gimme
      cart2.referenceNumber must === ("BR10002")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new Customer_Seed {
      val cart =
        Carts.create(Cart(accountId = customer.accountId, referenceNumber = "R123456")).gimme
      cart.referenceNumber must === ("R123456")
    }

    "can only have one cart per customer" in new Customer_Seed {
      val cart = Carts.create(Cart(accountId = customer.accountId)).gimme

      val failure = Carts
        .create(cart.copy(id = 0, referenceNumber = cart.refNum + "ZZZ"))
        .run()
        .futureValue
        .leftVal
      failure.getMessage must include(
          """value violates unique constraint "customer_has_only_one_cart"""")
    }

    "has a unique index on referenceNumber" in new Customer_Seed {
      val cart = Carts.create(Cart(accountId = customer.accountId)).gimme

      val failure = Carts.create(cart.copy(id = 0).copy(subTotal = 123)).run().futureValue.leftVal
      failure.getMessage must include(
          """value violates unique constraint "cords_reference_number_key"""")
    }
  }
}
