package services

import models._
import payloads.{CreateCreditCard, CreateAddressPayload}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils._

class CreditCardPaymentCreatorTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "CreditCardPaymentCreatorTest" - {
    "Adds a credit card with a billing address that does not exist in the address book" in new Fixture {
      val addressPayload = CreateAddressPayload(name = "Home Office", regionId = 1,
        street1 = "3000 Coolio Dr", city = "Seattle", zip = "55555")
      val ccPayload = CreateCreditCard(holderName = customer.firstName + " " + customer.lastName,
        number = "4242424242424242", cvv = "123", expMonth = 1, expYear = 2018, address = Some(addressPayload))

      val fullOrder = CreditCardPaymentCreator(order = order, customer = customer, cardPayload = ccPayload).run()
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
