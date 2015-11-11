package models

import java.time.Instant

import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._
import utils.time._

class RmaIntegrationTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "Rmas" - {
    "generates a referenceNumber in Postgres after insert when blank" in new Fixture {
      val rma = Rmas.saveNew(Rma.build(order, admin)).run().futureValue
      rma.referenceNumber must === ("ABC-123.1")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new Fixture {
      val rma = Rmas.saveNew(Rma.build(order, admin).copy(referenceNumber = "ABC-123.256")).run().futureValue
      rma.referenceNumber must === ("ABC-123.256")
    }

    "has a unique index on referenceNumber" in new Fixture {
      val rma = Rmas.saveNew(Rma.build(order, admin)).run().futureValue
      val failure = Rmas.saveNew(rma.copy(id = 0)).failed.run().futureValue
      failure.getMessage must include("""value violates unique constraint "rmas_reference_number_key"""")
    }
  }

  trait Fixture {
    val (admin, order) = (for {
      admin ← StoreAdmins.saveNew(Factories.storeAdmin)
      customer ← Customers.saveNew(Factories.customer)
      order ← Orders.saveNew(Factories.order.copy(
        referenceNumber = "ABC-123",
        status = Order.RemorseHold,
        customerId = customer.id,
        remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
    } yield (admin, order)).run().futureValue
  }
}
