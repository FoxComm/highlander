import objectframework.models.ObjectContexts
import org.json4s.jackson.JsonMethods._
import phoenix.failures.AddressFailures.NoCountryFound
import phoenix.models.cord.lineitems._
import phoenix.models.location.Addresses
import phoenix.models.product.{Mvp, SimpleContext}
import phoenix.models.rules.QueryStatement
import phoenix.models.shipping
import phoenix.models.shipping.{ShippingMethod, ShippingMethods}
import phoenix.responses.ShippingMethodsResponse.Root
import phoenix.services.carts.CartTotaler
import phoenix.utils.seeds.Factories
import testutils._
import testutils.apis.{PhoenixAdminApi, PhoenixStorefrontApi}
import testutils.fixtures.BakedFixtures
import core.db._
import phoenix.payloads.CartPayloads.CreateCart
import phoenix.responses.cord.CartResponse
import cats.implicits._
import faker.Lorem
import phoenix.payloads.LineItemPayloads.UpdateLineItemsPayload
import testutils.fixtures.api.ApiFixtures

class ShippingMethodsIntegrationTest
    extends IntegrationTestBase
    with PhoenixAdminApi
    with PhoenixStorefrontApi
    with DefaultJwtAdminAuth
    with BakedFixtures
    with ApiFixtures {

  "GET /v1/shipping-methods/:refNum" - {

    "Evaluates shipping rule: order total is greater than $25" - {

      "Shipping method is returned when actual order total is greater than $25" in new ShippingMethodsFixture {
        val conditions =
          parse("""
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
        val conditions =
          parse("""
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

  "Search /v1/my/cart/shipping-methods" - {

    "Has active methods" in new UsShipping {
      shippingMethodsApi.active().as[Seq[Root]].size mustBe >(0)
    }

    "Get shipping method by country code" in new UsShipping {
      withNewCustomerAuth(TestLoginData.random) { implicit auth ⇒
        cartsApi.create(CreateCart(customerId = auth.customerId.some)).as[CartResponse]
        storefrontCartsApi.shippingMethods.searchByRegion("us").as[Seq[Root]].size mustBe >(0)
      }
    }

    "Make sure that searchByRegion is aware of a cart content" in new UsShipping {
      withNewCustomerAuth(TestLoginData.random) { implicit auth ⇒
        val cart = cartsApi.create(CreateCart(customerId = auth.customerId.some)).as[CartResponse]

        storefrontCartsApi.shippingMethods
          .searchByRegion("us")
          .as[Seq[Root]]
          .exists(_.price == 0) mustBe false // no free shipping

        cartsApi(cart.referenceNumber).lineItems
          .add(Seq(UpdateLineItemsPayload(skuCode, 50))) // over 50 bucks
          .mustBeOk()

        storefrontCartsApi.shippingMethods
          .searchByRegion("us")
          .as[Seq[Root]]
          .exists(_.price == 0) mustBe true // YES! free shipping
      }
    }

    "No shipping to Russia ;(" in new UsShipping {
      withNewCustomerAuth(TestLoginData.random) { implicit auth ⇒
        cartsApi.create(CreateCart(customerId = auth.customerId.some)).as[CartResponse]
        storefrontCartsApi.shippingMethods.searchByRegion("rus").as[Seq[Root]].size must === (0)
      }
    }

    "No shipping methods for non existent country" in {
      withNewCustomerAuth(TestLoginData.random) { implicit auth ⇒
        cartsApi.create(CreateCart(customerId = auth.customerId.some)).as[CartResponse]
        storefrontCartsApi.shippingMethods
          .searchByRegion("uss")
          .mustFailWith400(NoCountryFound("uss"))
      }
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked with StoreAdmin_Seed

  trait ShippingMethodsFixture extends Fixture {
    val californiaId = 4129
    val michiganId   = 4148
    val oregonId     = 4164
    val washingtonId = 4177

    val address = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      address ← * <~ Addresses.create(
                 Factories.address.copy(accountId = customer.accountId, regionId = californiaId))
      address ← * <~ address.boundToCart(cart.refNum)
      product ← * <~ Mvp.insertProduct(productContext.id,
                                       Factories.products.head.copy(title = "Donkey", price = 27))
      _ ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = product.skuId))
      _ ← * <~ CartTotaler.saveTotals(cart)
    } yield address).gimme
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

  trait UsShipping extends ProductSku_ApiFixture {
    val shippingMethods: Seq[ShippingMethod] =
      Factories.shippingMethods.map(sm ⇒ shipping.ShippingMethods.create(sm).gimme)

    require(shippingMethods.length > 0)
  }
}
