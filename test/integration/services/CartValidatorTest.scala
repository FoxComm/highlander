package services

import models.{StoreAdmins, GiftCard, GiftCards, GiftCardManuals, Reasons, OrderPayments, OrderLineItem, Skus,
OrderLineItemSku, OrderLineItems, OrderLineItemSkus, CreditCards, Addresses, Customers, Orders, Order}
import services.CartFailures._
import util.{IntegrationTestBase, CatsHelpers}
import utils.Seeds.Factories
import utils.Slick.implicits._
import cats.implicits._
import slick.driver.PostgresDriver.api._

class CartValidatorTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  "CartValidator" - {
    "fails if the order is not a cart" in new Fixture {
      val nonCart = cart.copy(status = Order.RemorseHold)
      val result = leftValue(CartValidator(nonCart).validate.futureValue)

      result must === (OrderMustBeCart(nonCart.refNum).single)
    }

    "has warnings" - {
      "if the cart has no items" in new Fixture {
        val result = rightValue(CartValidator(cart).validate.futureValue)

        result.alerts mustBe 'empty
        result.warnings must contain(EmptyCart(cart.refNum))
      }

      "if the cart has no shipping address" in new Fixture {
        val result = rightValue(CartValidator(cart).validate.futureValue)

        result.alerts mustBe 'empty
        result.warnings must contain(NoShipAddress(cart.refNum))
      }

      "if the cart has no shipping method" in new Fixture {
        val result = rightValue(CartValidator(cart).validate.futureValue)

        result.alerts mustBe 'empty
        result.warnings must contain(NoShipMethod(cart.refNum))
      }

      "if the cart has line items but no payment methods" in new LineItemsFixture {
        val result = rightValue(CartValidator(cart).validate.futureValue)

        result.alerts mustBe 'empty
        result.warnings must contain(InsufficientFunds(cart.refNum))
      }

      "if the cart has no credit card and insufficient GC/SC available balances" in new LineItemsFixture {
        val notEnoughFunds = sku.price - 1

        (for {
          admin ← StoreAdmins.saveNew(Factories.storeAdmin)
          reason ← Reasons.saveNew(Factories.reason.copy(storeAdminId = admin.id))
          origin ← GiftCardManuals.saveNew(Factories.giftCardManual.copy(adminId = admin.id, reasonId = reason.id))
          giftCard ← GiftCards.saveNew(Factories.giftCard.copy(originId = origin.id, status = GiftCard.Active,
            originalBalance = notEnoughFunds))
          pmt ← OrderPayments.saveNew(Factories.giftCardPayment.copy(orderId = cart.id,
            amount = sku.price.some, paymentMethodId = giftCard.id))
        } yield pmt).run().futureValue

        val result = rightValue(CartValidator(cart).validate.futureValue)

        result.alerts mustBe 'empty
        result.warnings must contain(InsufficientFunds(cart.refNum))
      }
    }

    "never has warnings for sufficient funds" - {
      "if there is no grandTotal" in new Fixture {
        val result = rightValue(CartValidator(cart).validate.futureValue)

        result.alerts mustBe 'empty
        result.warnings mustNot contain(InsufficientFunds(cart.refNum))
      }

      "if the grandTotal == 0" in new LineItemsFixture {
        Skus.findById(sku.id).extract.map(_.price).update(0).run().futureValue
        val result = rightValue(CartValidator(cart).validate.futureValue)

        result.alerts mustBe 'empty
        result.warnings mustNot contain(InsufficientFunds(cart.refNum))
      }

      "if a credit card is present" in new CreditCartFixture with LineItemsFixture {
        val result = rightValue(CartValidator(cart).validate.futureValue)

        result.alerts mustBe 'empty
        result.warnings mustNot contain(InsufficientFunds(cart.refNum))
      }
    }
  }

  trait Fixture {
    val cart = Orders.saveNew(Factories.cart).run().futureValue
  }

  trait LineItemsFixture extends Fixture {
    val (sku, items) = (for {
      sku   ← Skus.saveNew(Factories.skus.head)
      _     ← OrderLineItemSkus.saveNew(OrderLineItemSku(skuId = sku.id, orderId = cart.id))
      items ← OrderLineItems.saveNew(OrderLineItem.buildSku(cart, sku))
    } yield (sku, items)).run().futureValue
  }

  trait CreditCartFixture extends Fixture {
    val (customer, cc) = (for {
      customer ← Customers.saveNew(Factories.customer)
      cc ← CreditCards.saveNew(Factories.creditCard.copy(customerId = customer.id))
      _ ← OrderPayments.saveNew(Factories.orderPayment.copy(orderId = cart.id, paymentMethodId = cc.id))
    } yield (customer, cc)).run().futureValue
  }
}

