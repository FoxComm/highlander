package models

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import services.GeneralFailure
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.jdbc.withUniqueConstraint

class OrderShippingAddressTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "OrderShippingAddress" - {
    "has only one shipping address per order" in new Fixture {
      val result = withUniqueConstraint {
        OrderShippingAddresses.save(shippingAddress.copy(name = "Yax2")).run()
      } { notUnique ⇒ GeneralFailure("There was already a shipping address") }

      result.futureValue mustBe 'bad
    }
  }

  trait Fixture {
    val (order, shippingAddress) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      shippingAddress ← OrderShippingAddresses.save(Factories.shippingAddress.copy(orderId = order.id))
    } yield (order, shippingAddress)).run().futureValue
  }
}
