package services

import cats.implicits._
import models.{CreditCards, Customers, GiftCard, GiftCardManual, GiftCardManuals, GiftCards, Order, OrderLineItem,
OrderLineItems, OrderPayment, OrderPayments, Orders, Reasons, StoreAdmins, StoreCredit, StoreCreditManual,
StoreCreditManuals, StoreCredits}
import models.product.{Products, ProductShadows, Skus, SkuShadows, ProductContexts, SimpleContext, Mvp}
import services.CartFailures._
import services.orders.OrderTotaler
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Slick.implicits._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import utils.Money.Currency
import Seeds.Factories
import utils.Slick.implicits._

class CartValidatorTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  "CartValidator" - {

    "has warnings" - {
      "if the cart has no items" in new Fixture {
        val result = CartValidator(refresh(cart)).validate().run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(EmptyCart(cart.refNum))
      }

      "if the cart has no shipping address" in new Fixture {
        val result = CartValidator(refresh(cart)).validate().run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(NoShipAddress(cart.refNum))
      }

      "if the cart has no shipping method" in new Fixture {
        val result = CartValidator(refresh(cart)).validate().run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(NoShipMethod(cart.refNum))
      }

      "if the cart has line items but no payment methods" in new LineItemsFixture {
        val result = CartValidator(refresh(cart)).validate().run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
      }

      "if the cart has no credit card and insufficient GC/SC available balances" in new LineItemsFixture {
        val skuPrice = Mvp.price(sku, skuShadow).getOrElse((0, Currency.USD))._1
        val notEnoughFunds = skuPrice - 1

        (for {
          admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
          reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
          origin   ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
          giftCard ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active,
            originalBalance = notEnoughFunds))
          payment  ← * <~ OrderPayments.create(Factories.giftCardPayment.copy(orderId = cart.id,
          amount = skuPrice.some, paymentMethodId = giftCard.id))
        } yield payment).runTxn().futureValue.rightVal

        val result = CartValidator(refresh(cart)).validate().run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
      }
    }

    "never has warnings for sufficient funds" - {
      "if there is no grandTotal" in new Fixture {
        val result = CartValidator(refresh(cart)).validate().run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "if the grandTotal == 0" in new LineItemsFixture {
        Skus.findById(sku.id).extract.update(sku.copy(attributes = Mvp.updatePrice(sku, skuShadow, 0))).run().futureValue
        OrderTotaler.saveTotals(cart).run().futureValue.rightVal

        val result = CartValidator(refresh(cart)).validate().run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "if a credit card is present" in new CreditCartFixture with LineItemsFixture {
        val result = CartValidator(refresh(cart)).validate().run().futureValue.rightVal

        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }
    }

    "has different rules for GC/SC pre-checkout and checkout funds check" - {
      "checks available balance for gift card before checkout" in new GiftCardFixture {
        val result = CartValidator(refresh(cart)).validate(isCheckout = false).run().futureValue.rightVal
        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "checks authorized funds for gift card during checkout" in new GiftCardFixture {
        // Does not approve sufficient available balance
        val result1 = CartValidator(refresh(cart)).validate(isCheckout = true).run().futureValue.rightVal
        result1.alerts mustBe 'empty
        result1.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
        // Approves authrorized funds
        GiftCards.auth(giftCard, orderPayment.id.some, grandTotal, 0).run().futureValue.rightVal
        val result2 = CartValidator(refresh(cart)).validate(isCheckout = true).run().futureValue.rightVal
        result2.alerts mustBe 'empty
        result2.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "checks available balance for store credit before checkout" in new StoreCreditFixture {
        val result = CartValidator(refresh(cart)).validate(isCheckout = false).run().futureValue.rightVal
        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "checks authorized funds for store credit during checkout" in new StoreCreditFixture {
        // Does not approve sufficient available balance
        val result1 = CartValidator(refresh(cart)).validate(isCheckout = true).run().futureValue.rightVal
        result1.alerts mustBe 'empty
        result1.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
        // Approves authrorized funds
        StoreCredits.auth(storeCredit, orderPayment.id.some, grandTotal).run().futureValue.rightVal
        val result2 = CartValidator(refresh(cart)).validate(isCheckout = true).run().futureValue.rightVal
        result2.alerts mustBe 'empty
        result2.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }
    }
  }

  trait Fixture {
    val cart = Orders.create(Factories.cart).run().futureValue.rightVal
  }

  trait LineItemsFixture extends Fixture {
    val (product, productShadow, sku, skuShadow, items) = (for {
      productContext ← * <~ ProductContexts.mustFindById404(SimpleContext.id)
      productData    ← * <~ Mvp.insertProduct(productContext.id, Factories.products.head)
      product        ← * <~ Products.mustFindById404(productData.productId)
      productShadow  ← * <~ ProductShadows.mustFindById404(productData.productShadowId)
      sku            ← * <~ Skus.mustFindById404(productData.skuId)
      skuShadow      ← * <~ SkuShadows.mustFindById404(productData.skuShadowId)
      items          ← * <~ OrderLineItems.create(OrderLineItem.buildSku(cart, sku))
      _              ← * <~ OrderTotaler.saveTotals(cart)
    } yield (product, productShadow, sku, skuShadow, items)).runTxn().futureValue.rightVal

    val grandTotal = refresh(cart).grandTotal
  }

  trait CreditCartFixture extends Fixture {
    val (customer, cc) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      cc       ← * <~ CreditCards.create(Factories.creditCard.copy(customerId = customer.id))
      _        ← * <~ OrderPayments.create(Factories.orderPayment.copy(orderId = cart.id, paymentMethodId = cc.id))
    } yield (customer, cc)).runTxn().futureValue.rightVal
  }

  trait GiftCardFixture extends LineItemsFixture {
    val (admin, giftCard, orderPayment) = (for {
      admin    ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason   ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      origin   ← * <~ GiftCardManuals.create(GiftCardManual(adminId = admin.id, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
      payment  ← * <~ OrderPayments.create(OrderPayment.build(giftCard).copy(orderId = cart.id, amount = grandTotal.some))
    } yield (admin, giftCard, payment)).run().futureValue.rightVal
  }

  trait StoreCreditFixture extends LineItemsFixture {
    val (admin, storeCredit, orderPayment) = (for {
      admin       ← * <~ StoreAdmins.create(Factories.storeAdmin)
      reason      ← * <~ Reasons.create(Factories.reason.copy(storeAdminId = admin.id))
      origin      ← * <~ StoreCreditManuals.create(StoreCreditManual(adminId = admin.id, reasonId = reason.id))
      storeCredit ← * <~ StoreCredits.create(Factories.storeCredit.copy(originId = origin.id, state = StoreCredit.Active))
      payment     ← * <~ OrderPayments.create(OrderPayment.build(storeCredit).copy(orderId = cart.id, amount = grandTotal.some))
    } yield (admin, storeCredit, payment)).run().futureValue.rightVal
  }

  def refresh(cart: Order) = Orders.refresh(cart).run().futureValue

}
