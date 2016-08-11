package models

import models.payment.creditcard.CreditCards
import util.{TestObjectContext, Fixtures, IntegrationTestBase}
import utils.db._
import utils.seeds.Seeds.Factories

class CreditCardIntegrationTest extends IntegrationTestBase with Fixtures with TestObjectContext {

  "CreditCard" - {
    "has only one default per customer" in new Fixture {
      val anotherDefault =
        CreditCards.create(cc.copy(id = 0, isDefault = true)).run().futureValue.leftVal
      anotherDefault.getMessage must include(
          "violates unique constraint \"credit_cards_default_idx\"")
    }
  }

  trait Fixture extends AddressFixture {
    val cc = CreditCards.create(Factories.creditCard.copy(customerId = customer.id)).gimme
  }
}
