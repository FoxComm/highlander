package models

import failures.DatabaseFailure
import models.account.Scope
import models.cord.Cart
import models.returns._
import testutils._
import testutils.fixtures.BakedFixtures
import utils.aliases._
import utils.db._

class ReturnIntegrationTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "Returns" - {
    "generates a referenceNumber in Postgres after insert when blank" in new Fixture {
      val rma = Returns.create(Return.build(order, storeAdmin)).gimme
      rma.referenceNumber must === ("BR10001.1")
    }

    "doesn't overwrite a non-empty referenceNumber after insert" in new Fixture {
      val rma =
        Returns.create(Return.build(order, storeAdmin).copy(referenceNumber = "ABC-123.256")).gimme
      rma.referenceNumber must === ("ABC-123.256")
    }

    "has a unique index on referenceNumber" in new Fixture {
      val rma     = Returns.create(Return.build(order, storeAdmin)).gimme
      val failure = Returns.create(rma.copy(id = 0)).gimmeFailures
      val errorMessage =
        "ERROR: duplicate key value violates unique constraint \"returns_reference_number_key\"\n" +
          "  Detail: Key (reference_number)=(BR10001.1) already exists."
      failure must === (DatabaseFailure(errorMessage).single)
    }
  }

  trait Fixture extends Order_Baked with StoreAdmin_Seed {
    Cart(referenceNumber = "ABC-123", scope = Scope.current, accountId = customer.accountId)
  }
}
