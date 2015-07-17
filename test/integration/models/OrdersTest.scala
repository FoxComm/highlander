package models

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import util.IntegrationTestBase
import utils.Seeds.Factories

class OrdersTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "Orders" - {
    "generates a referenceNumber in Postgres after insert when blank" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id, referenceNumber = "")).futureValue

      order.referenceNumber must === ("BR10001")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id, referenceNumber = "R123456")).futureValue
      order.referenceNumber must === ("R123456")
    }

    "can only have one record in 'cart' status" in new Fixture {
      val order = Orders.create(Factories.cart.copy(customerId = customer.id)).futureValue

      val failure = Orders.create(order.copy(id = 0)).failed.futureValue
      failure.getMessage must include( """value violates unique constraint "orders_has_only_one_cart"""")
    }
  }

  trait Fixture {
    val customer = Customers.save(Factories.customer).run().futureValue
  }
}
