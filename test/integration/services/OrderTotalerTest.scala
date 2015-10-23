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
      customer ← Customers.save(Factories.customer)
      address ← Addresses.save(Factories.address.copy(customerId = customer.id))
      order ← Orders.save(Factories.order.copy(customerId = customer.id))
    } yield (customer, address, order)).run().futureValue
  }

  trait SkuLineItemsFixture extends Fixture {
    val sku = (for {
      sku ← Skus.save(Factories.skus.head)
      _   ← OrderLineItemSkus.save(OrderLineItemSku(skuId = sku.id, orderId = order.id))
      _   ← OrderLineItems.save(OrderLineItem.buildSku(order, sku))
    } yield sku).run().futureValue
  }

  trait AllLineItemsFixture extends SkuLineItemsFixture {
    val (giftCard, lineItems) = (for {
      origin    ← GiftCardOrders.save(GiftCardOrder(orderId = order.id))
      giftCard  ← GiftCards.save(GiftCard.buildLineItem(balance = 150, originId = origin.id, currency = Currency.USD))
      gcLi      ← OrderLineItemGiftCards.save(OrderLineItemGiftCard(giftCardId = giftCard.id, orderId = order.id))

      lineItems ← OrderLineItems.save(OrderLineItem.buildGiftCard(order, gcLi))
    } yield (giftCard, lineItems)).run().futureValue
  }
}
