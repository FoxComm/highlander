import Extensions._
import akka.http.scaladsl.model.StatusCodes
import models.customer.Customers
import models.inventory.{Skus, Sku}
import models.location.Addresses
import models.order.{OrderShippingAddresses, Orders}
import models.order.lineitems._
import models.rules.QueryStatement
import models.product.{Mvp, SimpleContext, SimpleProductData}
import models.objects._
import models.shipping.ShippingMethods
import models.{shipping, StoreAdmins}
import org.json4s.jackson.JsonMethods._
import services.orders.OrderTotaler
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._
import utils.seeds.Seeds.Factories

import scala.concurrent.ExecutionContext.Implicits.global

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

        val action = shipping.ShippingMethods.create(Factories.shippingMethods.head.copy(
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

        val action = shipping.ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
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
    }
  }

  trait Fixture {
    val (order, storeAdmin, customer) = (for {
      customer   ← * <~ Customers.create(Factories.customer)
      order      ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
      storeAdmin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (order, storeAdmin, customer)).runTxn().futureValue.rightVal
  }

  trait ShippingMethodsFixture extends Fixture {
    val californiaId = 4129
    val michiganId = 4148
    val oregonId = 4164
    val washingtonId = 4177

    val (address, orderShippingAddress) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      address     ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id, regionId = californiaId))
      shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address, orderId = order.id)
      product     ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head.copy(title = "Donkey", price = 27))
      lineItemSku ← * <~ OrderLineItemSkus.safeFindBySkuId(product.skuId).toXor
      lineItems   ← * <~ OrderLineItems.create(OrderLineItem(orderId = order.id, originId = lineItemSku.id))
      _           ← * <~ OrderTotaler.saveTotals(order)
    } yield (address, shipAddress)).runTxn().futureValue.rightVal
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

    val action = ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
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

    val action = shipping.ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
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
      shippingMethod ← shipping.ShippingMethods.create(Factories.shippingMethods.head.copy(
        conditions = Some(conditions), restrictions = Some(restrictions)))
    } yield shippingMethod).run().futureValue.rightVal
  }

}
