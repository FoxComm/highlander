package models

import java.time.Instant

import scala.concurrent.ExecutionContext.Implicits.global

import failures.DatabaseFailure
import models.customer.Customers
import models.order.{Order, Orders}
import models.returns._
import util.IntegrationTestBase
import utils.db._
import utils.seeds.Seeds.Factories
import utils.time._

class ReturnIntegrationTest extends IntegrationTestBase {

  "Returns" - {
    "generates a referenceNumber in Postgres after insert when blank" in new Fixture {
      val rma = Returns.create(Return.build(order, admin)).gimme
      rma.referenceNumber must === ("ABC-123.1")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new Fixture {
      val rma =
        Returns.create(Return.build(order, admin).copy(referenceNumber = "ABC-123.256")).gimme
      rma.referenceNumber must === ("ABC-123.256")
    }

    "has a unique index on referenceNumber" in new Fixture {
      val rma     = Returns.create(Return.build(order, admin)).gimme
      val failure = Returns.create(rma.copy(id = 0)).run().futureValue.leftVal
      val errorMessage =
        "ERROR: duplicate key value violates unique constraint \"returns_reference_number_key\"\n" +
          "  Detail: Key (reference_number)=(ABC-123.1) already exists."
      failure must === (DatabaseFailure(errorMessage).single)
    }
  }

  trait Fixture {
    val (admin, order) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      customer ← * <~ Customers.create(Factories.customer)
      order ← * <~ Orders.create(
                 Factories.order.copy(referenceNumber = "ABC-123",
                                      state = Order.RemorseHold,
                                      customerId = customer.id,
                                      remorsePeriodEnd = Some(Instant.now.plusMinutes(30))))
    } yield (admin, order)).gimme
  }
}
