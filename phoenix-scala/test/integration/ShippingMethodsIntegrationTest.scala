import models.cord.OrderShippingAddresses
import models.cord.lineitems._
import models.location.Addresses
import models.objects._
import models.product.{Mvp, SimpleContext}
import models.rules.QueryStatement
import models.shipping
import models.shipping.ShippingMethods
import org.json4s.jackson.JsonMethods._
import responses.ShippingMethodsResponse.Root
import services.carts.CartTotaler
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class ShippingMethodsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with AutomaticAuth
    with BakedFixtures {

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

        val shippingMethod = shipping.ShippingMethods
          .create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
          .gimme

        val methodResponse = shippingMethodsApi.forCart(cart.refNum).as[Seq[Root]].headOption.value
        methodResponse.id must === (shippingMethod.id)
        methodResponse.name must === (shippingMethod.adminDisplayName)
        methodResponse.price must === (shippingMethod.price)
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

        val shippingMethod = shipping.ShippingMethods
          .create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
          .gimme

        shippingMethodsApi.forCart(cart.refNum).as[Seq[Root]] mustBe 'empty
      }
    }

    "Evaluates shipping rule: shipping to CA, OR, or WA" - {

      "Shipping method is returned when the order is shipped to CA" in new WestCoastShippingMethodsFixture {
        val methodResponse = shippingMethodsApi.forCart(cart.refNum).as[Seq[Root]].headOption.value

        methodResponse.id must === (shippingMethod.id)
        methodResponse.name must === (shippingMethod.adminDisplayName)
        methodResponse.price must === (shippingMethod.price)
      }
    }

    "Evaluates shipping rule: order total is between $10 and $100, and is shipped to CA, OR, or WA" - {

      "Is true when the order total is $27 and shipped to CA" in new ShippingMethodsStateAndPriceCondition {
        val methodResponse = shippingMethodsApi.forCart(cart.refNum).as[Seq[Root]].headOption.value

        methodResponse.id must === (shippingMethod.id)
        methodResponse.name must === (shippingMethod.adminDisplayName)
        methodResponse.price must === (shippingMethod.price)
      }
    }

    "Evaluates shipping rule: ships to CA but has a restriction for hazardous items" - {

      "Shipping method is returned when the order has no hazardous SKUs" in new ShipToCaliforniaButNotHazardous {
        val methodResponse = shippingMethodsApi.forCart(cart.refNum).as[Seq[Root]].headOption.value

        methodResponse.id must === (shippingMethod.id)
        methodResponse.name must === (shippingMethod.adminDisplayName)
        methodResponse.price must === (shippingMethod.price)
        methodResponse.isEnabled must === (true)
      }
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed

  trait ShippingMethodsFixture extends Fixture {
    val californiaId = 4129
    val michiganId   = 4148
    val oregonId     = 4164
    val washingtonId = 4177

    val (address, orderShippingAddress) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      address ← * <~ Addresses.create(
                   Factories.address.copy(accountId = customer.accountId, regionId = californiaId))
      shipAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                cordRef = cart.refNum)
      product ← * <~ Mvp.insertProduct(productContext.id,
                                       Factories.products.head.copy(title = "Donkey", price = 27))
      _ ← * <~ CartLineItems.create(
             CartLineItem(cordRef = cart.refNum, productVariantId = product.variantId))
      _ ← * <~ CartTotaler.saveTotals(cart)
    } yield (address, shipAddress)).gimme
  }

  trait WestCoastShippingMethodsFixture extends ShippingMethodsFixture {
    val conditions = parse(s"""
         |{
         |"comparison": "or",
         |"conditions": [
         |{
         |"rootObject": "ShippingAddress",
         |"field": "regionId",
         |"operator": "equals",
         |"valInt": $californiaId
          |}, {
          |"rootObject": "ShippingAddress",
          |"field": "regionId",
          |"operator": "equals",
          |"valInt": $oregonId
          |}, {
          |"rootObject": "ShippingAddress",
          |"field": "regionId",
          |"operator": "equals",
          |"valInt": $washingtonId
          |}
          |]
          |}
        """.stripMargin).extract[QueryStatement]

    val shippingMethod = ShippingMethods
      .create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
      .gimme
  }

  trait ShippingMethodsStateAndPriceCondition extends ShippingMethodsFixture {
    val conditions = parse(s"""
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
         |"valInt": $californiaId
          |}, {
          |"rootObject": "ShippingAddress",
          |"field": "regionId",
          |"operator": "equals",
          |"valInt": $oregonId
          |}, {
          |"rootObject": "ShippingAddress",
          |"field": "regionId",
          |"operator": "equals",
          |"valInt": $washingtonId
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

    val shippingMethod = shipping.ShippingMethods
      .create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
      .gimme
  }

  trait ShipToCaliforniaButNotHazardous extends ShippingMethodsFixture {
    val conditions = parse(s"""
         |{
         |"comparison": "and",
         |"conditions": [
         |{
         |"rootObject": "ShippingAddress",
         |"field": "regionId",
         |"operator": "equals",
         |"valInt": $californiaId
          |}
          |]
          |}
       """.stripMargin).extract[QueryStatement]

    val restrictions = parse("""
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
      shippingMethod ← shipping.ShippingMethods.create(
                          Factories.shippingMethods.head.copy(conditions = Some(conditions),
                                                              restrictions = Some(restrictions)))
    } yield shippingMethod).gimme
  }
}
