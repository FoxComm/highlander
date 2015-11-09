package services

import models._

import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import util.IntegrationTestBase
import utils.Seeds.Factories
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
      customer ← Customers.saveNew(Factories.customer)
      address ← Addresses.saveNew(Factories.address.copy(customerId = customer.id))
      order ← Orders.saveNew(Factories.order.copy(customerId = customer.id))
    } yield (customer, address, order)).run().futureValue
  }

  trait SkuLineItemsFixture extends Fixture {
    val sku = (for {
      sku ← Skus.saveNew(Factories.skus.head)
      _   ← OrderLineItemSkus.saveNew(OrderLineItemSku(skuId = sku.id, orderId = order.id))
      _   ← OrderLineItems.saveNew(OrderLineItem.buildSku(order, sku))
    } yield sku).run().futureValue
  }

  trait AllLineItemsFixture extends SkuLineItemsFixture {
    val (giftCard, lineItems) = (for {
      origin    ← GiftCardOrders.saveNew(GiftCardOrder(orderId = order.id))
      giftCard  ← GiftCards.saveNew(GiftCard.buildLineItem(balance = 150, originId = origin.id, currency = Currency.USD))
      gcLi      ← OrderLineItemGiftCards.saveNew(OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = order.id))

      lineItems ← OrderLineItems.saveNew(OrderLineItem.buildGiftCard(order, gcLi))
    } yield (giftCard, lineItems)).run().futureValue
  }
}
