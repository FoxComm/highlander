package services

import cats.implicits._
import models.{GiftCardManual, CreditCards, Customers, GiftCard, GiftCardManuals, GiftCards, Order, OrderLineItem,
OrderLineItemSku, OrderLineItemSkus, OrderLineItems, OrderPayments, Orders, Reasons, Skus, StoreAdmins}
import services.CartFailures._
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._

class CartValidatorTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  "CartValidator" - {

    "has warnings" - {
      "if the cart has no items" in new Fixture {
        val result = CartValidator(cart).validate.run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(EmptyCart(cart.refNum))
      }

      "if the cart has no shipping address" in new Fixture {
        val result = CartValidator(cart).validate.run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(NoShipAddress(cart.refNum))
      }

      "if the cart has no shipping method" in new Fixture {
        val result = CartValidator(cart).validate.run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(NoShipMethod(cart.refNum))
      }

      "if the cart has line items but no payment methods" in new LineItemsFixture {
        val result = CartValidator(cart).validate.run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
      }

      "if the cart has no credit card and insufficient GC/SC available balances" in new LineItemsFixture {
        val notEnoughFunds = sku.price - 1

        (for {
          admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
          reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
          origin   ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
          giftCard ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, status = GiftCard.Active,
            originalBalance = notEnoughFunds))
          payment  ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = cart.id,
            amount = sku.price.some, paymentMethodId = giftCard.id))
        } yield payment).runT().futureValue.rightVal

        val result = CartValidator(cart).validate.run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
      }
    }

    "never has warnings for sufficient funds" - {
      "if there is no grandTotal" in new Fixture {
        val result = CartValidator(cart).validate.run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "if the grandTotal == 0" in new LineItemsFixture {
        Skus.findById(sku.id).extract.map(_.price).update(0).run().futureValue
        val result = CartValidator(cart).validate.run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "if a credit card is present" in new CreditCartFixture with LineItemsFixture {
        val result = CartValidator(cart).validate.run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }
    }
  }

  trait Fixture {
    val cart = Orders.create(Factories.cart).run().futureValue.rightVal
  }

  trait LineItemsFixture extends Fixture {
    val (sku, items) = (for {
      sku   ← * <~ Skus.create(Factories.skus.head)
      _     ← * <~ OrderLineItemSkus.create(OrderLineItemSku(skuId = sku.id, orderId = cart.id))
      items ← * <~ OrderLineItems.create(OrderLineItem.buildSku(cart, sku))
    } yield (sku, items)).runT().futureValue.rightVal
  }

  trait CreditCartFixture extends Fixture {
    val (customer, cc) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      cc       ← * <~ CreditCards.create(Factories.creditCard.copy(customerId = customer.id))
      _        ← * <~ OrderPayments.create(Factories.orderPayment.copy(orderId = cart.id, paymentMethodId = cc.id))
    } yield (customer, cc)).runT().futureValue.rightVal
  }
}
