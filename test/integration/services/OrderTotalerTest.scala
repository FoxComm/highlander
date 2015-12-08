package services

import models.{Addresses, Customers, GiftCard, GiftCardOrder, GiftCardOrders, GiftCards, OrderLineItem,
OrderLineItemGiftCard, OrderLineItemGiftCards, OrderLineItemSku, OrderLineItemSkus, OrderLineItems, Orders, Skus}
import services.orders.OrderTotaler
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency
import utils.Slick.implicits._
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

        subTotal must === (sku.price + giftCard.originalBalance)
      }

      "uses SKU line items only if order purchases no gift cards" in new SkuLineItemsFixture {
        val subTotal = OrderTotaler.subTotal(order).run().futureValue

        subTotal must === (sku.price)
      }
    }

    "taxes" - {
      "are hardcoded to 5%" in new SkuLineItemsFixture {
        val totals = OrderTotaler.totals(order).run().futureValue
        val taxes = (sku.price * 0.05).toInt

        totals.subTotal === sku.price
        totals.shipping === 0
        totals.taxes === taxes
        totals.adjustments === 0
        totals.total === (totals.subTotal + taxes)
      }
    }

    "totals" - {
      "all are zero when there are no line items and no adjustments" in new Fixture {
        val totals = OrderTotaler.totals(order).run().futureValue

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
    } yield (customer, address, order)).runT().futureValue.rightVal
  }

  trait SkuLineItemsFixture extends Fixture {
    val sku = (for {
      sku ← * <~ Skus.create(Factories.skus.head)
      _   ← * <~ OrderLineItemSkus.create(OrderLineItemSku(skuId = sku.id, orderId = order.id))
      _   ← * <~ OrderLineItems.create(OrderLineItem.buildSku(order, sku))
    } yield sku).runT().futureValue.rightVal
  }

  trait AllLineItemsFixture extends SkuLineItemsFixture {
    val (giftCard, lineItems) = (for {
      origin    ← * <~ GiftCardOrders.create(GiftCardOrder(orderId = order.id))
      giftCard  ← * <~ GiftCards.create(GiftCard.buildLineItem(balance = 150, originId = origin.id, currency = Currency.USD))
      gcLi      ← * <~ OrderLineItemGiftCards.create(OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = order.id))
      lineItems ← * <~ OrderLineItems.create(OrderLineItem.buildGiftCard(order, gcLi))
    } yield (giftCard, lineItems)).runT().futureValue.rightVal
  }
}
