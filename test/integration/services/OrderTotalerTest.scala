package services

import models.customer.Customers
import models.inventory.Skus
import models.product.{Mvp, SimpleContext, SimpleProductData}
import models.objects._
import models.location.Addresses
import models.order.{OrderShippingMethod, OrderShippingMethods, Orders}
import models.order.lineitems._
import models.payment.giftcard.{GiftCardOrders, GiftCardOrder, GiftCards, GiftCard}
import models.shipping.{ShippingMethod, ShippingMethods}
import services.orders.OrderTotaler
import util.IntegrationTestBase
import utils.db._
import utils.db.DbResultT._
import utils.Money.Currency
import utils.seeds.Seeds.Factories

class OrderTotalerTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "OrderTotalerTest" - {
    "subTotal" - {
      "is zero when there are no line items" in new Fixture {
        val subTotal = OrderTotaler.subTotal(order).run().futureValue

        subTotal === 0
      }

      "includes both SKU line items and purchased gift cards" in new AllLineItemsFixture {
        val subTotal = OrderTotaler.subTotal(order).run().futureValue

        subTotal must === (skuPrice + giftCard.originalBalance)
      }

      "uses SKU line items only if order purchases no gift cards" in new SkuLineItemsFixture {
        val subTotal = OrderTotaler.subTotal(order).run().futureValue

        subTotal must === (skuPrice)
      }
    }

    "shipping" - {
      "sums the shipping total from both shipping methods" in new ShippingMethodFixture {
        val subTotal = OrderTotaler.shippingTotal(order).run().futureValue.rightVal
        subTotal must === (295)
      }
    }

    "taxes" - {
      "are hardcoded to 5%" in new SkuLineItemsFixture {
        val totals = OrderTotaler.totals(order).run().futureValue.rightVal
        val taxes = (skuPrice * 0.05).toInt

        totals.subTotal === skuPrice
        totals.shipping === 0
        totals.taxes === taxes
        totals.adjustments === 0
        totals.total === (totals.subTotal + taxes)
      }
    }

    "totals" - {
      "all are zero when there are no line items and no adjustments" in new Fixture {
        val totals = OrderTotaler.totals(order).run().futureValue.rightVal

        totals.subTotal mustBe 0
        totals.shipping mustBe 0
        totals.taxes mustBe 0
        totals.adjustments mustBe 0
        totals.total mustBe 0
      }
    }
  }

  trait Fixture {
    val (customer, address, order) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      address  ← * <~ Addresses.create(Factories.address.copy(customerId = customer.id))
      order    ← * <~ Orders.create(Factories.order.copy(customerId = customer.id))
    } yield (customer, address, order)).runTxn().futureValue.rightVal
  }

  trait SkuLineItemsFixture extends Fixture {
    val (productContext, product, productShadow, sku, skuShadow, skuPrice) = (for {
      productContext ← * <~ ObjectContexts.mustFindById404(SimpleContext.id)
      simpleProduct ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
      tup ← * <~ Mvp.getProductTuple(simpleProduct)
      _   ← * <~ OrderLineItems.create(OrderLineItem.buildSku(order, tup.sku))
      skuPrice ← * <~ Mvp.priceAsInt(tup.skuForm, tup.skuShadow)
    } yield (productContext, tup.product, tup.productShadow, tup.sku, tup.skuShadow, skuPrice)).runTxn().futureValue.rightVal
  }

  trait AllLineItemsFixture extends SkuLineItemsFixture {
    val (giftCard, lineItems) = (for {
      origin    ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = order.id))
      giftCard  ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = 150, originId = origin.id, currency = Currency.USD))
      gcLi      ← * <~ OrderLineItemGiftCards.create(OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = order.id))
      lineItems ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(order, gcLi))
    } yield (giftCard, lineItems)).runTxn().futureValue.rightVal
  }

  trait ShippingMethodFixture extends Fixture {
    val orderShippingMethods = (for {
      shipM ← * <~ ShippingMethods.create(Factories.shippingMethods.head.copy(price = 295))
      osm   ← * <~ OrderShippingMethods.create(OrderShippingMethod.build(order, shipM))
    } yield osm).runTxn().futureValue.rightVal
  }
}
