package services

import models._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils._
import utils.ExPostgresDriver.jsonMethods._

class ShippingManagerTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "ShippingManager" - {

    "Evaluates rule: shipped to CA, OR, or WA" - {

      "Is true when the order is shipped to CA" in new CaliforniaOrderFixture {
        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get.head must === (shippingMethod)
      }

      "Is true when the order is shipped to WA" in new WashingtonOrderFixture {
        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get.head must === (shippingMethod)
      }

      "Is false when the order is shipped to MI" in new MichiganOrderFixture {
        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get mustBe 'empty
      }

    }

    "Evaluates rule: order total is greater than $25" - {

      "Is true when the order total is greater than $25" in new PriceConditionFixture {
        val matchingMethods = ShippingManager.getShippingMethodsForOrder(expensiveOrder).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get.head must === (shippingMethod)
      }

      "Is false when the order total is less than $25" in new PriceConditionFixture {
        val matchingMethods = ShippingManager.getShippingMethodsForOrder(cheapOrder).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get mustBe 'empty
      }

    }

    "Evaluates rule: order total is between $10 and $100, and is shipped to WA, CA, or OR" - {

      "Is true when the order total is $27 and shipped to CA" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get.head must === (shippingMethod)
      }

      "Is false when the order total is $27 and shipped to MI" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = michiganId))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get mustBe 'empty
      }

    }

    "Evaluates rule: order total is greater than $10 and is not shipped to a P.O. Box" - {

      // TODO (Jeff): Need to support case insensitivity.

      "Is true when the order total is greater than $10 and no address field contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get.head must === (shippingMethod)
      }

      "Is false when the order total is greater than $10 and street1 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = washingtonId,
            street1 = "P.O. Box 1234"))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get mustBe 'empty
      }

      "Is false when the order total is greater than $10 and street2 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← Addresses.save(Factories.address.copy(customerId = customer.id, regionId = washingtonId,
            street2 = Some("P.O. Box 1234")))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).run().futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).futureValue
        matchingMethods.isRight must === (true)
        matchingMethods.get mustBe 'empty
      }

    }

  }

  trait Fixture {
    val (customer, order) = (for {
      customer ← Customers.save(Factories.customer)
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
      sku ← Skus.save(Sku(name = Some("Donkey"), price = 27))
      lineItem ← OrderLineItems.save(OrderLineItem(orderId = order.id, skuId = sku.id))
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
    val conditions =
      s"""
        | {
        |   "comparison": "or",
        |   "conditions": [
        |     {
        |       "rootObject": "ShippingAddress",
        |       "field": "regionId",
        |       "operator": "equals",
        |       "valInt": ${californiaId}
        |     }, {
        |       "rootObject": "ShippingAddress",
        |       "field": "regionId",
        |       "operator": "equals",
        |       "valInt": ${oregonId}
        |     }, {
        |       "rootObject": "ShippingAddress",
        |       "field": "regionId",
        |       "operator": "equals",
        |       "valInt": ${washingtonId}
        |     }
        |   ]
        | }
      """.stripMargin

    val action = ShippingMethods.save(Factories.shippingMethods.head.copy(conditions = Some(parse(conditions))))
    val shippingMethod = db.run(action).futureValue
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
    val conditions =
      """
        | {
        |   "comparison": "and",
        |   "conditions": [
        |     { "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 10 },
        |     { "rootObject": "ShippingAddress", "field": "street1", "operator": "notContains", "valString": "P.O. Box" },
        |     { "rootObject": "ShippingAddress", "field": "street2", "operator": "notContains", "valString": "P.O. Box" },
        |     { "rootObject": "ShippingAddress", "field": "street1", "operator": "notContains", "valString": "PO Box" },
        |     { "rootObject": "ShippingAddress", "field": "street2", "operator": "notContains", "valString": "PO Box" },
        |     { "rootObject": "ShippingAddress", "field": "street1", "operator": "notContains", "valString": "p.o. box" },
        |     { "rootObject": "ShippingAddress", "field": "street2", "operator": "notContains", "valString": "p.o. box" },
        |     { "rootObject": "ShippingAddress", "field": "street1", "operator": "notContains", "valString": "po box" },
        |     { "rootObject": "ShippingAddress", "field": "street2", "operator": "notContains", "valString": "po box" }
        |   ]
        | }
      """.stripMargin

    val action = ShippingMethods.save(Factories.shippingMethods.head.copy(conditions = Some(parse(conditions))))
    val shippingMethod = db.run(action).futureValue
  }

  trait PriceConditionFixture extends Fixture {
    val conditions =
      """
        | {
        |   "comparison": "and",
        |   "conditions": [{
        |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 25
        |   }]
        | }
      """.stripMargin

    val (shippingMethod, cheapOrder, expensiveOrder) = (for {
      shippingMethod ← ShippingMethods.save(Factories.shippingMethods.head.copy(conditions = Some(parse(conditions))))
      cheapOrder ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "CS1234-AA"))
      cheapSku ← Skus.save(Sku(name = Some("Cheap Donkey"), price = 10))
      cheapLineItem ← OrderLineItems.save(OrderLineItem(orderId = cheapOrder.id, skuId = cheapSku.id))
      cheapAddress ← Addresses.save(Factories.address.copy(customerId = customer.id, isDefaultShipping = false))
      _ ← OrderShippingAddresses.copyFromAddress(address = cheapAddress, orderId = cheapOrder.id)
      expensiveOrder ← Orders.save(Factories.order.copy(customerId = customer.id, referenceNumber = "CS1234-AA"))
      expensiveSku ← Skus.save(Sku(name = Some("Expensive Donkey"), price = 100))
      expensiveLineItem ← OrderLineItems.save(OrderLineItem(orderId = expensiveOrder.id, skuId = expensiveSku.id))
      expensiveAddress ← Addresses.save(Factories.address.copy(customerId = customer.id, isDefaultShipping = false))
      _ ← OrderShippingAddresses.copyFromAddress(address = expensiveAddress, orderId = expensiveOrder.id)
    } yield(shippingMethod, cheapOrder, expensiveOrder)).run().futureValue
  }

  trait StateAndPriceCondition extends Fixture {
    val conditions =
      s"""
        | {
        |   "comparison": "and",
        |   "statements": [
        |     {
        |       "comparison": "or",
        |       "conditions": [
        |         {
        |           "rootObject": "ShippingAddress",
        |           "field": "regionId",
        |           "operator": "equals",
        |           "valInt": ${californiaId}
        |         }, {
        |           "rootObject": "ShippingAddress",
        |           "field": "regionId",
        |           "operator": "equals",
        |           "valInt": ${oregonId}
        |         }, {
        |           "rootObject": "ShippingAddress",
        |           "field": "regionId",
        |           "operator": "equals",
        |           "valInt": ${washingtonId}
        |         }
        |       ]
        |     }, {
        |       "comparison": "and",
        |       "conditions": [
        |         {
        |           "rootObject": "Order",
        |           "field": "grandtotal",
        |           "operator": "greaterThanOrEquals",
        |           "valInt": 10
        |         }, {
        |           "rootObject": "Order",
        |           "field": "grandtotal",
        |           "operator": "lessThan",
        |           "valInt": 100
        |         }
        |       ]
        |     }
        |   ]
        | }
      """.stripMargin

    val action = ShippingMethods.save(Factories.shippingMethods.head.copy(conditions = Some(parse(conditions))))
    val shippingMethod = db.run(action).futureValue
  }
}
