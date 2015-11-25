package models

import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._

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
    } yield (customer, cc)).runT().futureValue.rightVal
  }
}
