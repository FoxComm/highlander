package services

import models._
import org.scalactic.{Bad, Good}
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils._

class ShippingManagerTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "ShippingManager" - {

    "Evaluates rule: shipped to CA, OR, or WA" - {

      "Succeeds when the order is shipped to CA" in new CaliforniaFixture {
        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (true)
      }

      "Succeeds when the order is shipped to WA" in new WashingtonFixture {
        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (true)
      }

    }

  }

  trait Fixture {
    val customer = (for {
      customer ← Customers.save(Factories.customer)
    } yield customer).run().futureValue
  }

  trait WestCoastFixture extends Fixture {
    val (california, oregon, washington, order) = (for {
      california ← States.save(State(id = 0, name = "California", abbreviation = "CA"))
      oregon ← States.save(State(id = 0, name = "Oregon", abbreviation = "OR"))
      washington ← States.save(State(id = 0, name = "Washington", abbreviation = "WA"))
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
    } yield (california, oregon, washington, order)).run().futureValue

    val stateConditions = Seq(
      Condition(rootObject = "ShippingAddress", field = "stateId",
                operator = Condition.Equals, valInt = Some(california.id)),
      Condition(rootObject = "ShippingAddress", field = "stateId",
                operator = Condition.Equals, valInt = Some(oregon.id)),
      Condition(rootObject = "ShippingAddress", field = "stateId",
                operator = Condition.Equals, valInt = Some(washington.id))
    )

    val conditionStatement = ConditionStatement(comparison = ConditionStatement.Or, conditions = stateConditions)
  }

  trait CaliforniaFixture extends WestCoastFixture {
    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = california.id))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).run().futureValue
  }

  trait WashingtonFixture extends WestCoastFixture {
    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = washington.id))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).run().futureValue
  }
}
