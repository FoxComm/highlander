package services

import models._
import models.rules.QueryStatement
import util.{IntegrationTestBase, CatsHelpers}
import utils.Seeds.Factories
import utils._
import utils.ExPostgresDriver.jsonMethods._
import utils.Slick.implicits._

class ShippingManagerTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global
  implicit val formats = JsonFormatters.phoenixFormats

  "ShippingManager" - {

    "Evaluates rule: shipped to CA, OR, or WA" - {

      "Is true when the order is shipped to WA" in new WashingtonOrderFixture {
        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods).head.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order is shipped to MI" in new MichiganOrderFixture {
        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods) mustBe 'empty
      }

    }

    "Evaluates rule: order total is greater than $25" - {

      "Is true when the order total is greater than $25" in new PriceConditionFixture {
        val matchingMethods = ShippingManager.getShippingMethodsForOrder(expensiveOrder).run().futureValue
        rightValue(matchingMethods).head.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order total is less than $25" in new PriceConditionFixture {
        val matchingMethods = ShippingManager.getShippingMethodsForOrder(cheapOrder).run().futureValue
        rightValue(matchingMethods) mustBe 'empty
      }

    }

    "Evaluates rule: order total is between $10 and $100, and is shipped to WA, CA, or OR" - {

      "Is true when the order total is $27 and shipped to CA" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = db.run(for {
          address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods).head.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order total is $27 and shipped to MI" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = db.run(for {
          address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, regionId = michiganId))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods) mustBe 'empty
      }

    }

    "Evaluates rule: order total is greater than $10 and is not shipped to a P.O. Box" - {

      "Is true when the order total is greater than $10 and no address field contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = db.run(for {
          address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods).head.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order total is greater than $10 and address1 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = db.run(for {
          address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, regionId = washingtonId,
            address1 = "P.O. Box 1234"))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods) mustBe 'empty
      }

      "Is false when the order total is greater than $10 and address2 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = db.run(for {
          address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, regionId = washingtonId,
            address2 = Some("P.O. Box 1234")))
          orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).futureValue

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods) mustBe 'empty
      }

    }

  }

  trait Fixture {
    val (customer, order) = db.run(for {
      customer ← Customers.saveNew(Factories.customer)
      order ← Orders.saveNew(Factories.order.copy(customerId = customer.id))
      sku ← Skus.saveNew(Factories.skus.head.copy(name = Some("Donkey"), price = 27))
      lineItemSku ← OrderLineItemSkus.saveNew(OrderLineItemSku(skuId = sku.id, orderId = order.id))
      lineItem ← OrderLineItems.saveNew(OrderLineItem(orderId = order.id, originId = lineItemSku.id,
        originType = OrderLineItem.SkuItem))
    } yield (customer, order)).futureValue

    val californiaId = 4129
    val michiganId = 4148
    val oregonId = 4164
    val washingtonId = 4177
  }

  trait OrderFixture extends Fixture {
    val (address, orderShippingAddress) = db.run(for {
      address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, regionId = californiaId))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).futureValue
  }

  trait WestCoastConditionFixture extends Fixture {
    val conditions = parse(
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
      """.stripMargin).extract[QueryStatement]

    val action = ShippingMethods.saveNew(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = db.run(action).futureValue
  }

  trait CaliforniaOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = db.run(for {
      address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, regionId = californiaId))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).futureValue
  }

  trait WashingtonOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = db.run(for {
      address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).futureValue
  }

  trait MichiganOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = db.run(for {
      address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, regionId = michiganId))
      orderShippingAddress ← OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).futureValue
  }

  trait POCondition extends Fixture {
val conditions = parse(
  """
    | {
    |   "comparison": "and",
    |   "conditions": [
    |     { "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 10 },
    |     { "rootObject": "ShippingAddress", "field": "address1", "operator": "notContains", "valString": "P.O. Box" },
    |     { "rootObject": "ShippingAddress", "field": "address2", "operator": "notContains", "valString": "P.O. Box" },
    |     { "rootObject": "ShippingAddress", "field": "address1", "operator": "notContains", "valString": "PO Box" },
    |     { "rootObject": "ShippingAddress", "field": "address2", "operator": "notContains", "valString": "PO Box" },
    |     { "rootObject": "ShippingAddress", "field": "address1", "operator": "notContains", "valString": "p.o. box" },
    |     { "rootObject": "ShippingAddress", "field": "address2", "operator": "notContains", "valString": "p.o. box" },
    |     { "rootObject": "ShippingAddress", "field": "address1", "operator": "notContains", "valString": "po box" },
    |     { "rootObject": "ShippingAddress", "field": "address2", "operator": "notContains", "valString": "po box" }
    |   ]
    | }
  """.stripMargin).extract[QueryStatement]

    val action = ShippingMethods.saveNew(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = db.run(action).futureValue
  }

  trait PriceConditionFixture extends Fixture {
    val conditions = parse(
      """
        | {
        |   "comparison": "and",
        |   "conditions": [{
        |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 25
        |   }]
        | }
      """.stripMargin).extract[QueryStatement]

    val (shippingMethod, cheapOrder, expensiveOrder) = db.run(for {
      shippingMethod ← ShippingMethods.saveNew(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
      cheapOrder ← Orders.saveNew(Factories.order.copy(customerId = customer.id, referenceNumber = "CS1234-AA"))
      cheapSku ← Skus.saveNew(Factories.skus.head.copy(name = Some("Cheap Donkey"), price = 10))
      cheapLineItemSku ← OrderLineItemSkus.saveNew(OrderLineItemSku(skuId = cheapSku.id, orderId = cheapOrder.id))
      cheapLineItem ← OrderLineItems.saveNew(OrderLineItem(orderId = cheapOrder.id, originId = cheapLineItemSku.id,
        originType = OrderLineItem.SkuItem))
      cheapAddress ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, isDefaultShipping = false))
      _ ← OrderShippingAddresses.copyFromAddress(address = cheapAddress, orderId = cheapOrder.id)
      expensiveOrder ← Orders.saveNew(Factories.order.copy(customerId = customer.id, referenceNumber = "CS1234-AA"))
      expensiveSku ← Skus.saveNew(Factories.skus.head.copy(name = Some("Expensive Donkey"), price = 100))
      expensiveLineItemSku ← OrderLineItemSkus.saveNew(OrderLineItemSku(skuId = expensiveSku.id,
        orderId = expensiveOrder.id))
      expensiveLineItem ← OrderLineItems.saveNew(OrderLineItem(orderId = expensiveOrder.id,
        originId = expensiveLineItemSku.id, originType = OrderLineItem.SkuItem))
      expensiveAddress ← Addresses.saveNew(Factories.address.copy(customerId = customer.id, isDefaultShipping = false))
      _ ← OrderShippingAddresses.copyFromAddress(address = expensiveAddress, orderId = expensiveOrder.id)
    } yield(shippingMethod, cheapOrder, expensiveOrder)).futureValue
  }

  trait StateAndPriceCondition extends Fixture {
    val conditions = parse(
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
      """.stripMargin).extract[QueryStatement]

    val action = ShippingMethods.saveNew(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = db.run(action).futureValue
  }
}
