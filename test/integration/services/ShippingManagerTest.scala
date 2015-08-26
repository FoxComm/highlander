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
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matches = ShippingManager.evaluateStatement(order, statement).futureValue
        matches must === (true)
      }

      "Is false when the order total is $27 and shipped to MI" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = michiganId))
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
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (true)
      }

      "Is false when the order total is greater than $10 and street1 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = washingtonId,
            street1 = "P.O. Box 1234"))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (false)
      }

      "Is false when the order total is greater than $10 and street2 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = washingtonId,
            street2 = Some("P.O. Box 1234")))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matches = ShippingManager.evaluateStatement(order, conditionStatement).futureValue
        matches must === (false)
      }

    }

  }

  trait Fixture {
    val (customer, order) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
    } yield (customer, order)).run().futureValue

    val californiaId = 4129
    val michiganId = 4148
    val oregonId = 4164
    val washingtonId = 4177
  }

  trait OrderFixture extends Fixture {
    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = californiaId))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).run().futureValue
  }

  trait WestCoastConditionFixture extends Fixture {
    val stateConditions = Seq(
      Condition(rootObject = "ShippingAddress", field = "regionId",
        operator = Condition.Equals, valInt = Some(californiaId)),
      Condition(rootObject = "ShippingAddress", field = "regionId",
        operator = Condition.Equals, valInt = Some(oregonId)),
      Condition(rootObject = "ShippingAddress", field = "regionId",
        operator = Condition.Equals, valInt = Some(washingtonId))
    )

    val conditionStatement = QueryStatement(comparison = QueryStatement.Or, conditions = stateConditions)
  }

  trait CaliforniaOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = californiaId))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).run().futureValue
  }

  trait WashingtonOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).run().futureValue
  }

  trait MichiganOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = michiganId))
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
