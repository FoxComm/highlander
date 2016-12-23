package services

import cats.implicits._
import models.account._
import models.cord.lineitems._
import models.cord.{Carts, OrderShippingAddresses}
import models.customer._
import models.location.Addresses
import models.objects._
import models.product.{Mvp, SimpleContext}
import models.rules.QueryStatement
import models.shipping.ShippingMethods
import services.ShippingManager.getShippingMethodsForCart
import services.carts.CartTotaler
import testutils._
import testutils.fixtures.BakedFixtures
import utils._
import utils.db.ExPostgresDriver.api._
import utils.db.ExPostgresDriver.jsonMethods._
import utils.db._
import utils.seeds.Seeds.Factories
import utils.seeds.ShipmentSeeds

class ShippingManagerTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  implicit val formats = JsonFormatters.phoenixFormats

  "ShippingManager" - {

    "Evaluates rule: shipped to CA, OR, or WA" - {

      "Is true when the order is shipped to WA" in new WashingtonOrderFixture {
        val matchingMethods = getShippingMethodsForCart(cart.refNum).gimme
        matchingMethods.head.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order is shipped to MI" in new MichiganOrderFixture {
        val matchingMethods = getShippingMethodsForCart(cart.refNum).gimme
        matchingMethods mustBe 'empty
      }
    }

    "Evaluates rule: shipped to Canada" - {
      "Is true when the order is shipped to Canada" in new CountryFixture {
        val canada = Addresses
          .create(
              Factories.address.copy(accountId = customer.accountId,
                                     name = "Mr Moose",
                                     regionId = ontarioId,
                                     isDefaultShipping = false))
          .gimme
        OrderShippingAddresses.filter(_.id === shippingAddress.id).delete.run().futureValue
        OrderShippingAddresses.copyFromAddress(address = canada, cordRef = cart.refNum).gimme

        val matchingMethods = getShippingMethodsForCart(cart.refNum).gimme
        matchingMethods.headOption.value.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order is shipped to US" in new CountryFixture {
        val matchingMethods = getShippingMethodsForCart(cart.refNum).gimme
        matchingMethods mustBe 'empty
      }
    }

    "Evaluates rule: order total is greater than $25" - {

      "Is true when the order total is greater than $25" in new PriceConditionFixture {
        val matchingMethods = getShippingMethodsForCart(expensiveCart.refNum).gimme
        matchingMethods.head.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order total is less than $25" in new PriceConditionFixture {
        val matchingMethods = getShippingMethodsForCart(cheapCart.refNum).gimme
        matchingMethods mustBe 'empty
      }
    }

    "Evaluates rule: order total is between $10 and $100, and is shipped to WA, CA, or OR" - {

      "Is true when the order total is $27 and shipped to CA" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId,
                                                                 regionId = washingtonId))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                             cordRef = cart.refNum)
        } yield (address, orderShippingAddress)).gimme

        val matchingMethods = getShippingMethodsForCart(cart.refNum).gimme
        matchingMethods.head.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order total is $27 and shipped to MI" in new StateAndPriceCondition {
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId,
                                                                 regionId = michiganId))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                             cordRef = cart.refNum)
        } yield (address, orderShippingAddress)).gimme

        val matchingMethods = getShippingMethodsForCart(cart.refNum).gimme
        matchingMethods mustBe 'empty
      }
    }

    "Evaluates rule: order total is greater than $10 and is not shipped to a P.O. Box" - {

      "Is true when the order total is greater than $10 and no address field contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(Factories.address.copy(accountId = customer.accountId,
                                                                 regionId = washingtonId))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                             cordRef = cart.refNum)
        } yield (address, orderShippingAddress)).gimme

        val matchingMethods = getShippingMethodsForCart(cart.refNum).gimme
        matchingMethods.headOption.value.name must === (shippingMethod.adminDisplayName)
      }

      "Is false when the order total is greater than $10 and address1 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(
                       Factories.address.copy(accountId = customer.accountId,
                                              regionId = washingtonId,
                                              address1 = "P.O. Box 1234"))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                             cordRef = cart.refNum)
        } yield (address, orderShippingAddress)).gimme

        val matchingMethods = getShippingMethodsForCart(cart.refNum).gimme
        matchingMethods mustBe 'empty
      }

      "Is false when the order total is greater than $10 and address2 contains a P.O. Box" in new POCondition {
        val (address, orderShippingAddress) = (for {
          address ← * <~ Addresses.create(
                       Factories.address.copy(accountId = customer.accountId,
                                              regionId = washingtonId,
                                              address2 = Some("P.O. Box 1234")))
          orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                             cordRef = cart.refNum)
        } yield (address, orderShippingAddress)).gimme

        val matchingMethods = getShippingMethodsForCart(cart.refNum).gimme
        matchingMethods mustBe 'empty
      }
    }
  }

  trait Fixture extends StoreAdmin_Seed with Customer_Seed {
    val cart = (for {
      cart ← * <~ Carts.create(Factories.cart(Scope.current).copy(accountId = customer.accountId))
      product ← * <~ Mvp.insertProduct(ctx.id,
                                       Factories.products.head.copy(title = "Donkey", price = 27))
      _ ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, productVariantId = product.skuId))

      cart ← * <~ CartTotaler.saveTotals(cart)
    } yield cart).gimme

    val californiaId = 4129
    val michiganId   = 4148
    val oregonId     = 4164
    val washingtonId = 4177
    val ontarioId    = 548
  }

  trait OrderFixture extends Fixture {
    val (address, shippingAddress) = (for {
      address ← * <~ Addresses.create(
                   Factories.address.copy(accountId = customer.accountId, regionId = californiaId))
      shippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                    cordRef = cart.refNum)
    } yield (address, shippingAddress)).gimme
  }

  trait WestCoastConditionFixture extends Fixture {
    val conditions = parse(s"""
        | {
        |   "comparison": "or",
        |   "conditions": [
        |     {
        |       "rootObject": "ShippingAddress",
        |       "field": "regionId",
        |       "operator": "equals",
        |       "valInt": $californiaId
        |     }, {
        |       "rootObject": "ShippingAddress",
        |       "field": "regionId",
        |       "operator": "equals",
        |       "valInt": $oregonId
        |     }, {
        |       "rootObject": "ShippingAddress",
        |       "field": "regionId",
        |       "operator": "equals",
        |       "valInt": $washingtonId
        |     }
        |   ]
        | }
      """.stripMargin).extract[QueryStatement]

    val action =
      ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = action.gimme
  }

  trait CaliforniaOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← * <~ Addresses.create(
                   Factories.address.copy(accountId = customer.accountId, regionId = californiaId))
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                         cordRef = cart.refNum)
    } yield (address, orderShippingAddress)).gimme
  }

  trait WashingtonOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← * <~ Addresses.create(
                   Factories.address.copy(accountId = customer.accountId, regionId = washingtonId))
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                         cordRef = cart.refNum)
    } yield (address, orderShippingAddress)).gimme
  }

  trait MichiganOrderFixture extends WestCoastConditionFixture {
    val (address, orderShippingAddress) = (for {
      address ← * <~ Addresses.create(
                   Factories.address.copy(accountId = customer.accountId, regionId = michiganId))
      orderShippingAddress ← * <~ OrderShippingAddresses.copyFromAddress(address = address,
                                                                         cordRef = cart.refNum)
    } yield (address, orderShippingAddress)).gimme
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

    val action =
      ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = action.gimme
  }

  trait PriceConditionFixture extends StoreAdmin_Seed {
    val scope = Scope.current

    val conditions = parse(
        """
        | {
        |   "comparison": "and",
        |   "conditions": [{
        |     "rootObject": "Order", "field": "grandtotal", "operator": "greaterThan", "valInt": 25
        |   }]
        | }
      """.stripMargin).extract[QueryStatement]

    val (shippingMethod, cheapCart, expensiveCart) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      shippingMethod ← * <~ ShippingMethods.create(
                          Factories.shippingMethods.head.copy(conditions = Some(conditions)))
      account  ← * <~ Accounts.create(Account())
      customer ← * <~ Users.create(Factories.customer.copy(accountId = account.id))
      _ ← * <~ CustomersData.create(
             CustomerData(userId = customer.id, accountId = account.id, scope = Scope.current))
      cheapCart ← * <~ Carts.create(
                     Factories
                       .cart(Scope.current)
                       .copy(accountId = customer.accountId, referenceNumber = "CS1234-AA"))
      cheapProduct ← * <~ Mvp.insertProduct(productContext.id,
                                            Factories.products.head.copy(title = "Cheap Donkey",
                                                                         price = 10,
                                                                         code = "SKU-CHP"))
      _ ← * <~ CartLineItems.create(
             CartLineItem(cordRef = cheapCart.refNum, productVariantId = cheapProduct.skuId))

      cheapAddress ← * <~ Addresses.create(
                        Factories.address.copy(accountId = customer.accountId,
                                               isDefaultShipping = false))
      _ ← * <~ OrderShippingAddresses.copyFromAddress(address = cheapAddress,
                                                      cordRef = cheapCart.refNum)
      account2 ← * <~ Accounts.create(Account())
      customer2 ← * <~ Users.create(
                     Factories.customer.copy(accountId = account2.id, email = "foo@bar.baz".some))
      _ ← * <~ CustomersData.create(
             CustomerData(userId = customer2.id, accountId = account2.id, scope = Scope.current))
      expensiveCart ← * <~ Carts.create(
                         Factories
                           .cart(Scope.current)
                           .copy(accountId = customer2.accountId, referenceNumber = "CS1234-AB"))
      expensiveProduct ← * <~ Mvp.insertProduct(productContext.id,
                                                Factories.products.head.copy(title =
                                                                               "Expensive Donkey",
                                                                             price = 100,
                                                                             code = "SKU-EXP"))
      _ ← * <~ CartLineItems.create(
             CartLineItem(cordRef = expensiveCart.refNum, productVariantId = expensiveProduct.skuId))
      expensiveAddress ← * <~ Addresses.create(
                            Factories.address.copy(accountId = customer.accountId,
                                                   isDefaultShipping = false))
      _ ← * <~ OrderShippingAddresses.copyFromAddress(address = expensiveAddress,
                                                      cordRef = expensiveCart.refNum)

      cheapCart     ← * <~ CartTotaler.saveTotals(cheapCart)
      expensiveCart ← * <~ CartTotaler.saveTotals(expensiveCart)
    } yield (shippingMethod, cheapCart, expensiveCart)).gimme
  }

  trait StateAndPriceCondition extends Fixture {
    val conditions = parse(s"""
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
        |           "valInt": $californiaId
        |         }, {
        |           "rootObject": "ShippingAddress",
        |           "field": "regionId",
        |           "operator": "equals",
        |           "valInt": $oregonId
        |         }, {
        |           "rootObject": "ShippingAddress",
        |           "field": "regionId",
        |           "operator": "equals",
        |           "valInt": $washingtonId
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

    val action =
      ShippingMethods.create(Factories.shippingMethods.head.copy(conditions = Some(conditions)))
    val shippingMethod = action.gimme
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

    val action =
      ShippingMethods.create(shippingMethods.headOption.value.copy(conditions = Some(conditions)))
    val shippingMethod = action.gimme
  }
}
