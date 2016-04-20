package models

import models.customer.Customers
import models.location.Addresses
import models.payment.creditcard.CreditCards
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._
import utils.seeds.Seeds
import Seeds.Factories

class CreditCardIntegrationTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  "CreditCard" - {
    "has only one default per customer" in new Fixture {
      val anotherDefault = CreditCards.create(cc.copy(id = 0, isDefault = true)).run().futureValue.leftVal
      anotherDefault.getMessage must include ("violates unique constraint \"credit_cards_default_idx\"")
    }
  }

  trait Fixture {
    val (customer, cc) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      address  ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
      cc       ← * <~ CreditCards.create(Factories.creditCard.copy(customerId = customer.id))
    } yield (customer, cc)).runTxn().futureValue.rightVal
  }
}
