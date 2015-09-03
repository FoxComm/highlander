package services

import models._
import payloads.{CreateCreditCard, CreateAddressPayload}
import util.{IntegrationTestBase, StripeSupport}
import utils.Seeds.Factories
import utils._

class CreditCardManagerTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "CreditCardManagerTest" - {
    "Adds a credit card with a billing address that does not exist in the address book" ignore new Fixture {
      val addressPayload = CreateAddressPayload(name = "Home Office", regionId = 1,
        street1 = "3000 Coolio Dr", city = "Seattle", zip = "55555")
      val ccPayload = CreateCreditCard(holderName = customer.firstName + " " + customer.lastName,
        number = StripeSupport.successfulCard, cvv = "123", expMonth = 1, expYear = 2018, address = Some(addressPayload))

      val fullOrder = CreditCardManager(order = order, customer = customer, cardPayload = ccPayload).run()
        .futureValue.get
    }
  }

  trait Fixture {
    val (customer, order) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
    } yield (customer, order)).run().futureValue
  }
}
