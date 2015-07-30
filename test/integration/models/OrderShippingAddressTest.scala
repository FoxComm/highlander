package models

import util.IntegrationTestBase
import utils.Seeds.Factories

class OrderShippingAddressTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "OrderShippingAddress" - {
//    "has only one default per customer" in new Fixture {
//      val anotherDefault = OrderShippingAddresses.createFromAddress(address, isDefault = true).run().failed.futureValue
//      anotherDefault.getMessage must include ("violates unique constraint \"shipping_addresses_default_idx\"")
//    }
  }

  trait Fixture {
//    val (customer, address, shippingAddress) = (for {
//      customer ← Customers.save(Factories.customer)
//      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
//      shippingAddress ← OrderShippingAddresses.createFromAddress(address, isDefault = true)
//    } yield (customer, address, shippingAddress)).run().futureValue
  }
}
