package models

import scala.concurrent.ExecutionContext.Implicits.global

import failures.GeneralFailure
import models.customer.Customers
import models.order.{OrderShippingAddresses, Orders}
import util.IntegrationTestBase
import utils.db.DbResultT._
import utils.db._
import utils.jdbc._
import utils.seeds.Seeds.Factories

class OrderShippingAddressIntegrationTest extends IntegrationTestBase {

  "OrderShippingAddress" - {
    "has only one shipping address per order" in new Fixture {
      val result = swapDatabaseFailure {
        OrderShippingAddresses.create(shippingAddress.copy(name = "Yax2")).run()
      } { (NotUnique, GeneralFailure("There was already a shipping address")) }

      result.futureValue mustBe 'left
    }
  }

  trait Fixture {
    val (order, shippingAddress) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      order    ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      shippingAddress ← * <~ OrderShippingAddresses.create(
                           Factories.shippingAddress.copy(orderId = order.id))
    } yield (order, shippingAddress)).gimme
  }
}
