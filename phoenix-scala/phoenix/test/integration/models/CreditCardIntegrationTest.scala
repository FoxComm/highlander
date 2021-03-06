package models

import phoenix.models.payment.creditcard.CreditCards
import phoenix.utils.seeds.Factories
import testutils._
import testutils.fixtures.BakedFixtures

class CreditCardIntegrationTest extends IntegrationTestBase with BakedFixtures with TestObjectContext {

  "CreditCard" - {
    "has only one default per customer" in new Fixture {
      val anotherDefault = CreditCards.create(cc.copy(id = 0, isDefault = true)).gimmeFailures
      anotherDefault.getMessage must include("violates unique constraint \"credit_cards_default_idx\"")
    }
  }

  trait Fixture extends CustomerAddress_Baked {
    val cc = CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId)).gimme
  }
}
