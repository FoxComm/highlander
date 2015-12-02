package models

import cats.data.Xor
import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import services.GeneralFailure
import util.IntegrationTestBase
import utils.seeds.Seeds
import Seeds.Factories
import utils.jdbc._
import utils.Slick.implicits._

class OrderShippingAddressIntegrationTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

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
      customer ← Customers.create(Factories.customer).map(rightValue)
      order ← Orders.create(Factories.order.copy(customerId = customer.id)).map(rightValue)
      shippingAddress ← OrderShippingAddresses.create(Factories.shippingAddress.copy(orderId = order.id)).map(rightValue)
    } yield (order, shippingAddress)).run().futureValue
  }
}
