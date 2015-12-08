package services

import models.rules.QueryStatement
import models.{Addresses, Customers, OrderLineItem, OrderLineItemSku, OrderLineItemSkus, OrderLineItems, OrderShippingAddresses, Orders, ShippingMethods, Skus}

import services.orders.OrderTotaler
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.ExPostgresDriver.api._
import utils.ExPostgresDriver.jsonMethods._
import utils.Slick.implicits._
import utils._
import utils.seeds.Seeds.Factories
import utils.seeds.ShipmentSeeds

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

    "Evaluates rule: shipped to Canada" - {
      "Is true when the order is shipped to Canada" in new CountryFixture {
        val canada = Addresses.create(Factories.address.copy(customerId = customer.id,
          name = "Canada, Eh", regionId = ontarioId, isDefaultShipping = false)).run().futureValue.rightVal
        OrderShippingAddresses.filter(_.id === orderShippingAddress.id).delete.run().futureValue
        OrderShippingAddresses.copyFromAddress(address = canada, orderId = order.id).run().futureValue.rightVal

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue.rightVal
        matchingMethods.headOption.value.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order is shipped to US" in new CountryFixture {
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
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).runT().futureValue.rightVal

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods).head.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order total is $27 and shipped to MI" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = michiganId))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).runT().futureValue.rightVal

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods) mustBe 'empty
      }

    }

    "Evaluates rule: order total is greater than $10 and is not shipped to a P.O. Box" - {

      "Is true when the order total is greater than $10 and no address field contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).runT().futureValue.rightVal

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue.rightVal
        matchingMethods.headOption.value.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order total is greater than $10 and address1 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = washingtonId,
            address1 = "P.O. Box 1234"))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).runT().futureValue.rightVal

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods) mustBe 'empty
      }

      "Is false when the order total is greater than $10 and address2 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = washingtonId,
            address2 = Some("P.O. Box 1234")))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
        } yield (address, orderShippingAddress)).runT().futureValue.rightVal

        val matchingMethods = ShippingManager.getShippingMethodsForOrder(order).run().futureValue
        rightValue(matchingMethods) mustBe 'empty
      }

    }

  }

  trait Fixture {
    val (customer, order) = (for {
      customer    ← * <~ Customers.create(Factories.customer)
      order       ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      sku         ← * <~ Skus.create(Factories.skus.head.copy(name = Some("Donkey"), price = 27))
      lineItemSku ← * <~ OrderLineItemSkus.create(OrderLineItemSku(skuId = sku.id, orderId = order.id))
      lineItem    ← * <~ OrderLineItems.create(OrderLineItem(orderId = order.id, originId = lineItemSku.id,
        originType = OrderLineItem.SkuItem))

      order       ← * <~ OrderTotaler.saveTotals(order)
    } yield (customer, order)).runT().futureValue.rightVal

    val californiaId = 4129
    val michiganId = 4148
    val oregonId = 4164
    val washingtonId = 4177
    val ontarioId = 548
  }

  trait OrderFixture extends Fixture {
    val (address, orderShippingAddress) = (for {
      address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = californiaId))
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).runT().futureValue.rightVal
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

    val action = ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = action.run().futureValue.rightVal
  }

  trait CaliforniaOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = californiaId))
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).runT().futureValue.rightVal
  }

  trait WashingtonOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = washingtonId))
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).runT().futureValue.rightVal
  }

  trait MichiganOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = michiganId))
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
    } yield (address, orderShippingAddress)).runT().futureValue.rightVal
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

    val action = ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = action.run().futureValue.rightVal
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

    val (shippingMethod, cheapOrder, expensiveOrder) = (for {
      shippingMethod ← * <~ ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
      cheapOrder ← * <~ Orders.create(Factories.order.copy(customerId = customer.id, referenceNumber = "CS1234-AA"))
      cheapSku ← * <~ Skus.create(Factories.skus.head.copy(name = Some("Cheap Donkey"), price = 10))
      cheapLineItemSku ← * <~ OrderLineItemSkus.create(OrderLineItemSku(skuId = cheapSku.id, orderId = cheapOrder.id))
      cheapLineItem ← * <~ OrderLineItems.create(OrderLineItem(orderId = cheapOrder.id, originId = cheapLineItemSku.id,
        originType = OrderLineItem.SkuItem))
      cheapAddress ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, isDefaultShipping = false))
      _ ← * <~ OrderShippingAddresses.copyFromAddress(address = cheapAddress, orderId = cheapOrder.id)
      expensiveOrder ← * <~ Orders.create(Factories.order.copy(customerId = customer.id, referenceNumber = "CS1234-AB"))
      expensiveSku ← * <~ Skus.create(Factories.skus.head.copy(name = Some("Expensive Donkey"), price = 100))
      expensiveLineItemSku ← * <~ OrderLineItemSkus.create(OrderLineItemSku(skuId = expensiveSku.id,
        orderId = expensiveOrder.id))
      expensiveLineItem ← * <~ OrderLineItems.create(OrderLineItem(orderId = expensiveOrder.id,
        originId = expensiveLineItemSku.id, originType = OrderLineItem.SkuItem))
      expensiveAddress ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, isDefaultShipping = false))
      _ ← * <~ OrderShippingAddresses.copyFromAddress(address = expensiveAddress, orderId = expensiveOrder.id)

      cheapOrder      ← * <~ OrderTotaler.saveTotals(cheapOrder)
      expensiveOrder  ← * <~ OrderTotaler.saveTotals(expensiveOrder)
    } yield(shippingMethod, cheapOrder, expensiveOrder)).runT().futureValue.rightVal
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

    val action = ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = action.run().futureValue.rightVal
  }

  trait CountryFixture extends OrderFixture with ShipmentSeeds {
    val conditions = parse(
      """
        | {
        |   "comparison": "and",
        |   "conditions": [
        |     { "rootObject": "ShippingAddress", "field": "countryId", "operator": "equals", "valInt": 39 }
        |   ]
        | }
      """.stripMargin).extract[QueryStatement]

    val action = ShippingMethods.create(shippingMethods.headOption.value.copy(conditions = Some(conditions)))
    val shippingMethod = action.run().futureValue.rightVal
  }

}
