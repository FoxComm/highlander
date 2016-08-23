package models

import models.payment.creditcard.CreditCards
import util._
import util.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CreditCardIntegrationTest
    extends IntegrationTestBase
    with BakedFixtures
    with TestObjectContext {

  "CreditCard" - {
    "has only one default per customer" in new Fixture {
      val anotherDefault =
        CreditCards.create(cc.copy(id = 0, isDefault = true)).run().futureValue.leftVal
      anotherDefault.getMessage must include(
          "violates unique constraint \"credit_cards_default_idx\"")
    }
  }

  trait Fixture extends CustomerAddress_Baked {
    val cc = CreditCards.create(Factories.creditCard.copy(customerId = customer.id)).gimme
  }
}
