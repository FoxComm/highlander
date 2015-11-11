package models

import cats.data.Xor
import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import services.GeneralFailure
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.jdbc._
import utils.Slick.implicits._

class OrderShippingAddressIntegrationTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "OrderShippingAddress" - {
    "has only one shipping address per order" in new Fixture {
      pending // FIXME after #522

      val result = swapDatabaseFailure {
        OrderShippingAddresses.saveNew(shippingAddress.copy(name = "Yax2")).map(Xor.right).run()
      } { (NotUnique, GeneralFailure("There was already a shipping address")) }

      result.futureValue mustBe 'left
    }
  }

  trait Fixture {
    val (order, shippingAddress) = (for {
      customer ← Customers.saveNew(Factories.customer)
      order ← Orders.saveNew(Factories.order.copy(customerId = customer.id))
      shippingAddress ← OrderShippingAddresses.saveNew(Factories.shippingAddress.copy(orderId = order.id))
    } yield (order, shippingAddress)).run().futureValue
  }
}
