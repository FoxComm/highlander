package models

import scala.concurrent.ExecutionContext.Implicits.global

import failures.DatabaseFailure
import models.cord._
import models.customer.Customers
import models.returns._
import util._
import utils.db._
import utils.seeds.Seeds.Factories

class ReturnIntegrationTest extends IntegrationTestBase with TestObjectContext {

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
      cart ← * <~ Carts.create(
                Factories.cart.copy(referenceNumber = "ABC-123", customerId = customer.id))
      order ← * <~ Orders.create(cart.toOrder())
    } yield (admin, order)).gimme
  }
}
