package utils

import scala.concurrent.ExecutionContext.Implicits.global

import models.{Addresses, Customers}
import services.GeneralFailure
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class ModelIntegrationTest extends IntegrationTestBase {

  "New model create" - {
    "validates model" in {
      val failures = leftValue(Addresses.create(Factories.address.copy(zip = "totallyNotAValidZip")).run().futureValue)
      failures must === (GeneralFailure("zip must fully match regular expression '^\\d{5}(?:\\d{4})?$'").single)
    }

    "sanitizes model" in {
      val result = (for {
        customer ← Customers.save(Factories.customer)
        address ← Addresses.create(Factories.address.copy(zip = "123-45", customerId = customer.id))
      } yield address).run().futureValue
      rightValue(result).zip must === ("12345")
    }

    "catches exceptions from DB" in {
      val result = (for {
        customer ← Customers.save(Factories.customer)
        original ← Addresses.create(Factories.address.copy(customerId = customer.id))
        copycat ← Addresses.create(Factories.address.copy(customerId = customer.id))
      } yield copycat).run().futureValue
      leftValue(result) must === (GeneralFailure(
        "ERROR: duplicate key value violates unique constraint \"address_shipping_default_idx\"\n" +
        "  Detail: Key (customer_id, is_default_shipping)=(1, t) already exists.").single)
    }
  }
}
