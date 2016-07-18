package services

import models.customer.Customers
import models.location.Addresses
import models.objects._
import models.cord.lineitems._
import models.cord.{Carts, OrderShippingMethod, OrderShippingMethods}
import models.payment.giftcard.{GiftCard, GiftCardOrder, GiftCardOrders, GiftCards}
import models.product.{Mvp, SimpleContext}
import models.shipping.ShippingMethods
import services.carts.CartTotaler
import util.{IntegrationTestBase, TestObjectContext}
import utils.Money.Currency
import utils.db._
import utils.seeds.Seeds.Factories
import concurrent.ExecutionContext.Implicits.global

class CartTotalerTest extends IntegrationTestBase with TestObjectContext {

  "OrderTotalerTest" - {
    "subTotal" - {
      "is zero when there are no line items" in new Fixture {
        val subTotal = CartTotaler.subTotal(cart).run().futureValue

        subTotal === 0
      }

      "includes both SKU line items and purchased gift cards" in new AllLineItemsFixture {
        val subTotal = CartTotaler.subTotal(cart).run().futureValue

        subTotal must === (skuPrice + giftCard.originalBalance)
      }

      "uses SKU line items only if order purchases no gift cards" in new SkuLineItemsFixture {
        val subTotal = CartTotaler.subTotal(cart).run().futureValue

        subTotal must === (skuPrice)
      }
    }

    "shipping" - {
      "sums the shipping total from both shipping methods" in new ShippingMethodFixture {
        val subTotal = CartTotaler.shippingTotal(cart).gimme
        subTotal must === (295)
      }
    }

    "taxes" - {
      "are hardcoded to 5%" in new SkuLineItemsFixture {
        val totals = CartTotaler.totals(cart).gimme
        val taxes  = (skuPrice * 0.05).toInt

        totals.subTotal === skuPrice
        totals.shipping === 0
        totals.taxes === taxes
        totals.adjustments === 0
        totals.total === (totals.subTotal + taxes)
      }
    }

    "totals" - {
      "all are zero when there are no line items and no adjustments" in new Fixture {
        val totals = CartTotaler.totals(cart).gimme

        totals.subTotal mustBe 0
        totals.shipping mustBe 0
        totals.taxes mustBe 0
        totals.adjustments mustBe 0
        totals.total mustBe 0
      }
    }
  }

  trait Fixture {
    val (customer, address, cart) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      address  ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
      cart     ← * <~ Carts.create(Factories.cart.copy(customerId = customer.id))
    } yield (customer, address, cart)).gimme
  }

  trait SkuLineItemsFixture extends Fixture {
    val (productContext, product, productShadow, sku, skuShadow, skuPrice) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      simpleProduct  ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
      tup            ← * <~ Mvp.getProductTuple(simpleProduct)
      _              ← * <~ OrderLineItems.create(OrderLineItem.buildSku(cart, tup.sku))
      skuPrice       ← * <~ Mvp.priceAsInt(tup.skuForm, tup.skuShadow)
    } yield
      (productContext, tup.product, tup.productShadow, tup.sku, tup.skuShadow, skuPrice)).gimme
  }

  trait AllLineItemsFixture extends SkuLineItemsFixture {
    val (giftCard, lineItems) = (for {
      origin ← * <~ GiftCardOrders.create(GiftCardOrder(cordRef = cart.refNum))
      giftCard ← * <~ GiftCards.create(
                    GiftCard
                      .buildLineItem(balance = 150, originId = origin.id, currency = Currency.USD))
      gcLi ← * <~ OrderLineItemGiftCards.create(
                OrderLineItemGiftCard(giftCardId = giftCard.id, cordRef = cart.refNum))
      lineItems ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(cart, gcLi))
    } yield (giftCard, lineItems)).gimme
  }

  trait ShippingMethodFixture extends Fixture {
    val orderShippingMethods = (for {
      shipM ← * <~ ShippingMethods.create(Factories.shippingMethods.head.copy(price = 295))
      osm   ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(cart.refNum, shipM))
    } yield osm).gimme
  }
}
