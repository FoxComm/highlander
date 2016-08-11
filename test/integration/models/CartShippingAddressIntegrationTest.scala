package models

import failures.GeneralFailure
import models.cord.OrderShippingAddresses
import util.{TestObjectContext, Fixtures, IntegrationTestBase}
import utils.db._
import utils.jdbc._
import utils.seeds.Seeds.Factories

class CartShippingAddressIntegrationTest
    extends IntegrationTestBase
    with Fixtures
    with TestObjectContext {

  "OrderShippingAddress" - {
    "has only one shipping address per order" in new Fixture {
      val result = swapDatabaseFailure {
        OrderShippingAddresses.create(shippingAddress.copy(name = "Yax2")).run()
      } { (NotUnique, GeneralFailure("There was already a shipping address")) }

      result.futureValue mustBe 'left
    }
  }

  trait Fixture extends EmptyCustomerCartFixture {
    val shippingAddress =
      OrderShippingAddresses.create(Factories.shippingAddress.copy(cordRef = cart.refNum)).gimme
  }
}
