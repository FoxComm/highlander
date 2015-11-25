package services

import models._

import services.orders.OrderTotaler
import utils.Money.Currency
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._

class OrderTotalerTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "OrderTotalerTest" - {
    "subTotal" - {
      "isEmpty when there are no line items" in new Fixture {
        val subTotal = OrderTotaler.subTotal(order).run().futureValue

        subTotal mustBe 'empty
      }

      "includes both SKU line items and purchased gift cards" in new AllLineItemsFixture {
        val subTotal = OrderTotaler.subTotal(order).run().futureValue.value

        subTotal must === (sku.price + giftCard.originalBalance)
      }

      "uses SKU line items only if order purchases no gift cards" in new SkuLineItemsFixture {
        val subTotal = OrderTotaler.subTotal(order).run().futureValue.value

        subTotal must === (sku.price)
      }
    }

    "grandTotal" - {
      "isEmpty when there are no line items" in new Fixture {
        val grandTotal = OrderTotaler.grandTotal(order).run().futureValue

        grandTotal mustBe 'empty
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
