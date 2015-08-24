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

      "Is true when the order is shipped to CA" in new CaliforniaOrderFixture {
        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (true)
      }

      "Is true when the order is shipped to WA" in new WashingtonOrderFixture {
        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (true)
      }

      "Is false when the order is shipped to MI" in new MichiganOrderFixture {
        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (false)
      }

    }

    "Evaluates rule: order total is greater than $25" - {

      "Is true when the order total is greater than $25" in new OrderFixture {
        val orderTotalCondition = Condition(rootObject = "Order", field = "grandtotal",
          operator = Condition.GreaterThan, valInt = Some(25))

        val statement = QueryStatement(comparison = QueryStatement.And, conditions = Seq(orderTotalCondition))
        val matches = ShippingManager.evaluateStatement(order, statement).futureValue
        matches must ===(true)
      }

    }

    "Evaluates rule: order total is greater than $100" - {

      "Is false when the order total is less than $100" in new OrderFixture {
        val orderTotalCondition = Condition(rootObject = "Order", field = "grandtotal",
          operator = Condition.GreaterThan, valInt = Some(100))

        val statement = QueryStatement(comparison = QueryStatement.And, conditions = Seq(orderTotalCondition))
        val matches = ShippingManager.evaluateStatement(order, statement).futureValue
        matches must === (false)
      }

    }

    "Evaluates rule: order total is between $10 and $100, and is shipped to WA, CA, or OR" - {

      "Is true when the order total is $27 and shipped to CA" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = washington.id))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matches = ShippingManager.evaluateStatement(order, statement).futureValue
        matches must === (true)
      }

      "Is false when the order total is $27 and shipped to MI" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = michigan.id))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matches = ShippingManager.evaluateStatement(order, statement).futureValue
        matches must === (false)
      }

    }

    "Evaluates rule: order total is greater than $10 and is not shipped to a P.O. Box" - {

      // TODO (Jeff): Need to support case insensitivity.

      "Is true when the order total is greater than $10 and no address field contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = washington.id))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (true)
      }

      "Is false when the order total is greater than $10 and street1 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = washington.id,
            street1 = "P.O. Box 1234"))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (false)
      }

      "Is false when the order total is greater than $10 and street2 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = washington.id,
            street2 = Some("P.O. Box 1234")))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (false)
      }

    }

  }

  trait Fixture {
    val (customer, order, california, michigan, oregon, washington) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      california ← States.save(State(id = 0, name = "California", abbreviation = "CA"))
      michigan ← States.save(State(id = 0, name = "Michigan", abbreviation = "MI"))
      oregon ← States.save(State(id = 0, name = "Oregon", abbreviation = "OR"))
      washington ← States.save(State(id = 0, name = "Washington", abbreviation = "WA"))
    } yield (customer, order, california, michigan, oregon, washington)).run().futureValue
  }

  trait OrderFixture extends Fixture {
    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = california.id))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).run().futureValue
  }

  trait WestCoastConditionFixture extends Fixture {
    val stateConditions = Seq(
      Condition(rootObject = "ShippingAddress", field = "stateId",
        operator = Condition.Equals, valInt = Some(california.id)),
      Condition(rootObject = "ShippingAddress", field = "stateId",
        operator = Condition.Equals, valInt = Some(oregon.id)),
      Condition(rootObject = "ShippingAddress", field = "stateId",
        operator = Condition.Equals, valInt = Some(washington.id))
    )

    val conditionStatement = QueryStatement(comparison = QueryStatement.Or, conditions = stateConditions)
  }

  trait CaliforniaOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = california.id))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).run().futureValue
  }

  trait WashingtonOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = washington.id))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).run().futureValue
  }

  trait MichiganOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      michigan ← States.save(State(id = 0, name = "Michigan", abbreviation = "MI"))
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, stateId = michigan.id))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).run().futureValue
  }

  trait POCondition extends Fixture {
    val conditions = Seq(
      Condition(rootObject = "Order", field = "grandtotal",
        operator = Condition.GreaterThan, valInt = Some(10)),
      Condition(rootObject = "ShippingAddress", field = "street1",
        operator = Condition.NotContains, valString = Some("P.O. Box")),
      Condition(rootObject = "ShippingAddress", field = "street2",
        operator = Condition.NotContains, valString = Some("P.O. Box")),
      Condition(rootObject = "ShippingAddress", field = "street1",
        operator = Condition.NotContains, valString = Some("PO Box")),
      Condition(rootObject = "ShippingAddress", field = "street2",
        operator = Condition.NotContains, valString = Some("PO Box")),
      Condition(rootObject = "ShippingAddress", field = "street1",
        operator = Condition.NotContains, valString = Some("p.o. box")),
      Condition(rootObject = "ShippingAddress", field = "street2",
        operator = Condition.NotContains, valString = Some("p.o. box")),
      Condition(rootObject = "ShippingAddress", field = "street1",
        operator = Condition.NotContains, valString = Some("po box")),
      Condition(rootObject = "ShippingAddress", field = "street2",
        operator = Condition.NotContains, valString = Some("po box"))
    )

    val conditionStatement = QueryStatement(comparison = QueryStatement.And, conditions = conditions)
  }

  trait StateAndPriceCondition extends WestCoastConditionFixture {
    val priceConditions = Seq(
      Condition(rootObject = "Order", field = "grandtotal",
        operator = Condition.GreaterThanOrEquals, valInt = Some(10)),
      Condition(rootObject = "Order", field = "grandtotal",
        operator = Condition.LessThan, valInt = Some(100))
    )

    val priceStatement = QueryStatement(comparison = QueryStatement.And, conditions = priceConditions)
    val statement = QueryStatement(comparison = QueryStatement.And,
      statements = Seq(conditionStatement, priceStatement))
  }
}
