package models

import com.wix.accord.{Failure ⇒ ValidationFailure, Success ⇒ ValidationSuccess}
import services.GeneralFailure
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.jdbc.withUniqueConstraint

class OrderBillingAddressTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "OrderBillingAddress" - {
    "has only one billing address per order" in new Fixture {
      val result = withUniqueConstraint {
        OrderBillingAddresses.save(billingAddress.copy(name = "Jeff")).run()
      } { notUnique ⇒ GeneralFailure("There was already a billing address") }

      result.futureValue mustBe 'bad
    }
  }

  trait Fixture {
    val (order, billingAddress) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      billingAddress ← OrderBillingAddresses.save(Factories.billingAddress.copy(orderId = order.id))
    } yield (order, billingAddress)).run().futureValue
  }
}
