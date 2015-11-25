import scala.concurrent.ExecutionContext.Implicits.global
import akka.http.scaladsl.model.StatusCodes

import models.rules.QueryStatement
import models._
import org.json4s.jackson.JsonMethods._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Seeds.Factories
import utils.Slick.implicits._
import Extensions._

class ShippingMethodsIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  "GET /v1/shipping-methods/:refNum" - {

    "Evaluates shipping rule: order total is greater than $25" - {

      "Shipping method is returned when actual order total is greater than $25" in new ShippingMethodsFixture {
        val conditions = parse(
          """
            | {
            |   "comparison": "and",
            |   "conditions": [{
            |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 25
            |   }]
            | }
          """.stripMargin).extract[QueryStatement]

        val action = models.ShippingMethods.create(Factories.shippingMethods.head.copy(
          conditions = Some(conditions)))
        val shippingMethod = db.run(action).futureValue.rightVal

        val response = GET(s"v1/shipping-methods/${order.referenceNumber}")
        response.status must ===(StatusCodes.OK)

        val methodResponse = response.as[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must ===(shippingMethod.id)
        methodResponse.name must ===(shippingMethod.adminDisplayName)
        methodResponse.price must ===(shippingMethod.price)
      }

    }

    "Evaluates shipping rule: order total is greater than $100" - {

      "No shipping rules found when order total is less than $100" in new ShippingMethodsFixture {
        val conditions = parse(
          """
            | {
            |   "comparison": "and",
            |   "conditions": [{
            |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 100
            |   }]
            | }
          """.stripMargin).extract[QueryStatement]

        val action = models.ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
        val shippingMethod = db.run(action).futureValue.rightVal

        val response = GET(s"v1/shipping-methods/${order.referenceNumber}")
        response.status must ===(StatusCodes.OK)

        val methodResponse = response.as[Seq[responses.ShippingMethods.Root]]
        methodResponse mustBe 'empty
      }

    }

    "Evaluates shipping rule: shipping to CA, OR, or WA" - {

      "Shipping method is returned when the order is shipped to CA" in new WestCoastShippingMethodsFixture {
        val response = GET(s"v1/shipping-methods/${order.referenceNumber}")
        response.status must ===(StatusCodes.OK)

        val methodResponse = response.as[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must ===(shippingMethod.id)
        methodResponse.name must ===(shippingMethod.adminDisplayName)
        methodResponse.price must ===(shippingMethod.price)
      }
    }

    "Evaluates shipping rule: order total is between $10 and $100, and is shipped to CA, OR, or WA" - {

      "Is true when the order total is $27 and shipped to CA" in new ShippingMethodsStateAndPriceCondition {
        val response = GET(s"v1/shipping-methods/${order.referenceNumber}")
        response.status must ===(StatusCodes.OK)

        val methodResponse = response.as[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must ===(shippingMethod.id)
        methodResponse.name must ===(shippingMethod.adminDisplayName)
        methodResponse.price must ===(shippingMethod.price)
      }

    }

    "Evaluates shipping rule: ships to CA but has a restriction for hazardous items" - {

      "Shipping method is returned when the order has no hazardous SKUs" in new ShipToCaliforniaButNotHazardous {
        val response = GET(s"v1/shipping-methods/${order.referenceNumber}")
        response.status must ===(StatusCodes.OK)

        val methodResponse = response.as[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must ===(shippingMethod.id)
        methodResponse.name must ===(shippingMethod.adminDisplayName)
        methodResponse.price must ===(shippingMethod.price)
        methodResponse.isEnabled must ===(true)
      }

      "Shipping method is returned, but disabled with a hazardous SKU" in new ShipToCaliforniaButNotHazardous {
        (for {
          hazSku      ← * <~ Skus.create(Sku(sku = "HAZ-SKU", name = Some("fox"), price = 56, isHazardous = true))
          lineItemSku ← * <~ OrderLineItemSkus.create(OrderLineItemSku(orderId = order.id, skuId = hazSku.id))
          lineItem    ← * <~ OrderLineItems.create(OrderLineItem(orderId = order.id, originId = lineItemSku.id))
        } yield lineItem).runT().futureValue.rightVal

        val response = GET(s"v1/shipping-methods/${order.referenceNumber}")
        response.status must ===(StatusCodes.OK)

        val methodResponse = response.as[Seq[responses.ShippingMethods.Root]].head
        methodResponse.id must ===(shippingMethod.id)
        methodResponse.name must ===(shippingMethod.adminDisplayName)
        methodResponse.price must ===(shippingMethod.price)
        methodResponse.isEnabled must ===(false)
      }

    }
  }

  trait Fixture {
    val (order, storeAdmin, customer) = (for {
      customer   ← * <~ Customers.create(Factories.customer)
      order      ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (order, storeAdmin, customer)).runT().futureValue.rightVal
  }

  trait ShippingMethodsFixture extends Fixture {
    val californiaId = 4129
    val michiganId = 4148
    val oregonId = 4164
    val washingtonId = 4177

    val (address, orderShippingAddress) = (for {
      address     ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = californiaId))
      shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
      sku         ← * <~ Skus.create(Factories.skus.head.copy(name = Some("Donkey"), price = 27))
      lineItemSku ← * <~ OrderLineItemSkus.create(OrderLineItemSku(orderId = order.id, skuId = sku.id))
      lineItems   ← * <~ OrderLineItems.create(OrderLineItem(orderId = order.id, originId = lineItemSku.id))
    } yield (address, shipAddress)).runT().futureValue.rightVal
  }

  trait WestCoastShippingMethodsFixture extends ShippingMethodsFixture {
    val conditions = parse(
      s"""
         |{
         |"comparison": "or",
         |"conditions": [
         |{
         |"rootObject": "ShippingAddress",
         |"field": "regionId",
         |"operator": "equals",
         |"valInt": ${californiaId}
          |}, {
          |"rootObject": "ShippingAddress",
          |"field": "regionId",
          |"operator": "equals",
          |"valInt": ${oregonId}
          |}, {
          |"rootObject": "ShippingAddress",
          |"field": "regionId",
          |"operator": "equals",
          |"valInt": ${washingtonId}
          |}
          |]
          |}
        """.stripMargin).extract[QueryStatement]

    val action = models.ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = db.run(action).futureValue.rightVal
  }

  trait ShippingMethodsStateAndPriceCondition extends ShippingMethodsFixture {
    val conditions = parse(
      s"""
         |{
         |"comparison": "and",
         |"statements": [
         |{
         |"comparison": "or",
         |"conditions": [
         |{
         |"rootObject": "ShippingAddress",
         |"field": "regionId",
         |"operator": "equals",
         |"valInt": ${californiaId}
          |}, {
          |"rootObject": "ShippingAddress",
          |"field": "regionId",
          |"operator": "equals",
          |"valInt": ${oregonId}
          |}, {
          |"rootObject": "ShippingAddress",
          |"field": "regionId",
          |"operator": "equals",
          |"valInt": ${washingtonId}
          |}
          |]
          |}, {
          |"comparison": "and",
          |"conditions": [
          |{
          |"rootObject": "Order",
          |"field": "grandtotal",
          |"operator": "greaterThanOrEquals",
          |"valInt": 10
          |}, {
          |"rootObject": "Order",
          |"field": "grandtotal",
          |"operator": "lessThan",
          |"valInt": 100
          |}
          |]
          |}
          |]
          |}
      """.stripMargin).extract[QueryStatement]

    val action = models.ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = db.run(action).futureValue.rightVal
  }

  trait ShipToCaliforniaButNotHazardous extends ShippingMethodsFixture {
    val conditions = parse(
      s"""
         |{
         |"comparison": "and",
         |"conditions": [
         |{
         |"rootObject": "ShippingAddress",
         |"field": "regionId",
         |"operator": "equals",
         |"valInt": ${californiaId}
          |}
          |]
          |}
       """.stripMargin).extract[QueryStatement]

    val restrictions = parse(
      """
        | {
        |   "comparison": "and",
        |   "conditions": [
        |     {
        |       "rootObject": "Order",
        |       "field": "skus.isHazardous",
        |       "operator": "equals",
        |       "valBoolean": true
        |     }
        |   ]
        | }
      """.stripMargin).extract[QueryStatement]

    val shippingMethod = (for {
      shippingMethod ← models.ShippingMethods.create(Factories.shippingMethods.head.copy(
        conditions = Some(conditions), restrictions = Some(restrictions)))
    } yield shippingMethod).run().futureValue.rightVal
  }

}
