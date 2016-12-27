package services

import cats.implicits._
import failures.CartFailures._
import models.Reasons
import models.cord._
import models.cord.lineitems._
import models.inventory.ProductVariants
import models.objects._
import models.payment.creditcard.CreditCards
import models.payment.giftcard._
import models.payment.storecredit._
import models.product._
import services.carts.CartTotaler
import testutils._
import testutils.fixtures.BakedFixtures
import utils.db._
import utils.seeds.Seeds.Factories

class CartValidatorTest extends IntegrationTestBase with TestObjectContext with BakedFixtures {

  "CartValidator" - {

    "has warnings" - {
      "if the cart has no items" in new Fixture {
        val result = CartValidator(refresh(cart)).validate().gimme

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(EmptyCart(cart.refNum))
      }

      "if the cart has no shipping address" in new Fixture {
        val result = CartValidator(refresh(cart)).validate().gimme

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(NoShipAddress(cart.refNum))
      }

      "if the cart has no shipping method" in new Fixture {
        val result = CartValidator(refresh(cart)).validate().gimme

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(NoShipMethod(cart.refNum))
      }

      "if the cart has line items but no payment methods" in new LineItemsFixture {
        val result = CartValidator(refresh(cart)).validate().gimme

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
      }

      "if the cart has no credit card and insufficient GC/SC available balances" in new LineItemsFixture
      with StoreAdmin_Seed {
        val skuPrice       = Mvp.priceAsInt(skuForm, skuShadow)
        val notEnoughFunds = skuPrice - 1

        (for {
          reason ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
          origin ← * <~ GiftCardManuals.create(
                      GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
          giftCard ← * <~ GiftCards.create(
                        Factories.giftCard.copy(originId = origin.id,
                                                state = GiftCard.Active,
                                                originalBalance = notEnoughFunds))
          payment ← * <~ OrderPayments.create(
                       Factories.giftCardPayment.copy(cordRef = cart.refNum,
                                                      amount = notEnoughFunds.some,
                                                      paymentMethodId = giftCard.id))
        } yield payment).gimme

        val result = CartValidator(refresh(cart)).validate().gimme

        result.alerts mustBe 'empty
        result.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
      }
    }

    "never has warnings for sufficient funds" - {
      "if there is no grandTotal" in new Fixture {
        val result = CartValidator(refresh(cart)).validate().gimme

        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "if the grandTotal == 0" in new LineItemsFixture0 {
        CartTotaler.saveTotals(cart).gimme

        val result = CartValidator(refresh(cart)).validate().gimme

        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "if a credit card is present" in new CreditCartFixture with LineItemsFixture {
        val result = CartValidator(refresh(cart)).validate().gimme

        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }
    }

    "has different rules for GC/SC pre-checkout and checkout funds check" - {
      "checks available balance for gift card before checkout" in new GiftCardFixture {
        val result = CartValidator(refresh(cart)).validate(isCheckout = false).gimme
        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "checks authorized funds for gift card during checkout" in new GiftCardFixture {
        // Does not approve sufficient available balance
        val result1 = CartValidator(refresh(cart)).validate(isCheckout = true).gimme
        result1.alerts mustBe 'empty
        result1.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
        // Approves authrorized funds
        GiftCards.auth(giftCard, orderPayment.id.some, grandTotal, 0).gimme
        val result2 = CartValidator(refresh(cart)).validate(isCheckout = true).gimme
        result2.alerts mustBe 'empty
        result2.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "checks available balance for store credit before checkout" in new StoreCreditFixture {
        val result = CartValidator(refresh(cart)).validate(isCheckout = false).gimme
        result.alerts mustBe 'empty
        result.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }

      "checks authorized funds for store credit during checkout" in new StoreCreditFixture {
        // Does not approve sufficient available balance
        val result1 = CartValidator(refresh(cart)).validate(isCheckout = true).gimme
        result1.alerts mustBe 'empty
        result1.warnings.value.toList must contain(InsufficientFunds(cart.refNum))
        // Approves authrorized funds
        StoreCredits.auth(storeCredit, orderPayment.id.some, grandTotal).gimme
        val result2 = CartValidator(refresh(cart)).validate(isCheckout = true).gimme
        result2.alerts mustBe 'empty
        result2.warnings.value.toList mustNot contain(InsufficientFunds(cart.refNum))
      }
    }
  }

  trait Fixture extends EmptyCustomerCart_Baked

  trait LineItemsFixture extends Fixture {

    val (product, productForm, productShadow, sku, skuForm, skuShadow, items) = (for {
      productData   ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head)
      product       ← * <~ Products.mustFindById404(productData.productId)
      productForm   ← * <~ ObjectForms.mustFindById404(product.formId)
      productShadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)
      sku           ← * <~ ProductVariants.mustFindById404(productData.skuId)
      skuForm       ← * <~ ObjectForms.mustFindById404(sku.formId)
      skuShadow     ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      items         ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = sku.id))
      _             ← * <~ CartTotaler.saveTotals(cart)
    } yield (product, productForm, productShadow, sku, skuForm, skuShadow, items)).gimme

    val grandTotal = refresh(cart).grandTotal
  }

  trait LineItemsFixture0 extends Fixture {
    val (product, productForm, productShadow, sku, skuForm, skuShadow, items) = (for {
      productData   ← * <~ Mvp.insertProduct(ctx.id, Factories.products.head.copy(price = 0))
      product       ← * <~ Products.mustFindById404(productData.productId)
      productForm   ← * <~ ObjectForms.mustFindById404(product.formId)
      productShadow ← * <~ ObjectShadows.mustFindById404(product.shadowId)
      sku           ← * <~ ProductVariants.mustFindById404(productData.skuId)
      skuForm       ← * <~ ObjectForms.mustFindById404(sku.formId)
      skuShadow     ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      items         ← * <~ CartLineItems.create(CartLineItem(cordRef = cart.refNum, skuId = sku.id))
      _             ← * <~ CartTotaler.saveTotals(cart)
    } yield (product, productForm, productShadow, sku, skuForm, skuShadow, items)).gimme

    val grandTotal = refresh(cart).grandTotal
  }

  trait CreditCartFixture extends Fixture {
    val cc = (for {
      cc ← * <~ CreditCards.create(Factories.creditCard.copy(accountId = customer.accountId))
      _ ← * <~ OrderPayments.create(
             Factories.orderPayment.copy(cordRef = cart.refNum, paymentMethodId = cc.id))
    } yield cc).gimme
  }

  trait GiftCardFixture extends LineItemsFixture with StoreAdmin_Seed {
    val (giftCard, orderPayment) = (for {
      reason ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
      origin ← * <~ GiftCardManuals.create(
                  GiftCardManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      giftCard ← * <~ GiftCards.create(
                    Factories.giftCard.copy(originId = origin.id, state = GiftCard.Active))
      payment ← * <~ OrderPayments.create(
                   OrderPayment
                     .build(giftCard)
                     .copy(cordRef = cart.refNum, amount = grandTotal.some))
    } yield (giftCard, payment)).gimme
  }

  trait StoreCreditFixture extends LineItemsFixture with StoreAdmin_Seed {
    val (storeCredit, orderPayment) = (for {
      reason ← * <~ Reasons.create(Factories.reason(storeAdmin.accountId))
      origin ← * <~ StoreCreditManuals.create(
                  StoreCreditManual(adminId = storeAdmin.accountId, reasonId = reason.id))
      storeCredit ← * <~ StoreCredits.create(
                       Factories.storeCredit.copy(originId = origin.id,
                                                  state = StoreCredit.Active,
                                                  accountId = customer.accountId))
      payment ← * <~ OrderPayments.create(
                   OrderPayment
                     .build(storeCredit)
                     .copy(cordRef = cart.refNum, amount = grandTotal.some))
    } yield (storeCredit, payment)).gimme
  }

  def refresh(cart: Cart) = Carts.refresh(cart).gimme
}
